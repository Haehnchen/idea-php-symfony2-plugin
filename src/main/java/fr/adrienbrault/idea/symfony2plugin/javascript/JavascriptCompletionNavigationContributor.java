package fr.adrienbrault.idea.symfony2plugin.javascript;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.lang.javascript.JSTokenTypes;
import com.intellij.lang.javascript.patterns.JSElementPattern;
import com.intellij.lang.javascript.patterns.JSPatterns;
import com.intellij.lang.javascript.psi.*;
import com.intellij.lang.javascript.psi.resolve.JSResolveUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.patterns.InitialPatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class JavascriptCompletionNavigationContributor {

    private static final InsertHandler<LookupElement> INSERT_HANDLER_ROUTE_PATH_REPLACE = new MyLookupElementReplaceRouteWithPath();

    public static class Completion extends CompletionContributor {
        public Completion() {
            extend(CompletionType.BASIC, PlatformPatterns.psiElement().withElementType(JSTokenTypes.STRING_LITERAL).withParent(JSPatterns.jsLiteralExpression()), new CompletionParametersCompletionProvider());
        }

        private static class CompletionParametersCompletionProvider extends CompletionProvider<CompletionParameters> {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                PsiElement psiElement = parameters.getOriginalPosition();
                if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContext() instanceof JSLiteralExpression jsLiteral)) {
                    return;
                }

                if (!isAcceptedUrlPattern(jsLiteral)) {
                    return;
                }

                // add by resolved path
                result.addAllElements(RouteHelper.getRoutesPathLookupElements(psiElement.getProject()));

                // provide route name, but replace it by path on insert
                for (Route route : RouteHelper.getAllRoutes(psiElement.getProject()).values()) {
                    String path = route.getPath();
                    if (path != null) {
                        LookupElementBuilder lookupElementBuilder = LookupElementBuilder
                            .create(route, route.getName())
                            .withIcon(Symfony2Icons.SYMFONY)
                            .withTypeText(path)
                            .withInsertHandler(INSERT_HANDLER_ROUTE_PATH_REPLACE);

                        result.addElement(lookupElementBuilder);
                    }
                }
            }
        }
    }

    public static class GotoDeclaration implements GotoDeclarationHandler {
        @Override
        public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement psiElement, int offset, Editor editor) {
            if (PlatformPatterns.psiElement().withElementType(JSTokenTypes.STRING_LITERAL).withParent(JSPatterns.jsLiteralExpression()).accepts(psiElement)) {
                JSLiteralExpression parent = (JSLiteralExpression) psiElement.getParent();

                String content = parent.getStringValue();
                if (content != null && !content.isBlank()) {
                    return RouteHelper.getMethodsForPathWithPlaceholderMatch(psiElement.getProject(), content);
                }
            }

            return new PsiElement[0];
        }
    }

    /**
     * insertHandler are after already inserted string, so remove it and replace it :)
     */
    private static class MyLookupElementReplaceRouteWithPath implements InsertHandler<LookupElement> {
        @Override
        public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
            Document document = context.getDocument();

            document.deleteString(context.getStartOffset(), context.getTailOffset());

            Route route = (Route) item.getObject();
            document.insertString(context.getTailOffset(), route.getPath());
            context.commitDocument();

            context.getEditor().getCaretModel().moveToOffset(context.getTailOffset());
        }
    }

    private static boolean isAcceptedUrlPattern(@NotNull final JSLiteralExpression jsLiteral) {
        return
            // axios.create('')
            // axios.create(baseURL: '')
            JSPatterns.jsLiteralExpression()
                .inside(false, JSPatterns.jsProperty().withName("baseURL").withParent(JSObjectLiteralExpression.class))
                .inside(false, JSPatterns.or(
                    JSPatterns.jsArgument(JSPatterns.jsReferenceExpression().withQualifiedName("axios.create"), 0)
                ))
                .accepts(jsLiteral)

                || JSPatterns.jsLiteralExpression().inside(false, JSPatterns.jsArgument(JSPatterns.jsReferenceExpression().withQualifiedName("axios"), 0)).accepts(jsLiteral)
                || JSPatterns.jsLiteralExpression().inside(false, JSPatterns.jsArgument(JSPatterns.jsReferenceExpression().withQualifiedName("fetch"), 0)).accepts(jsLiteral)

                // new Request( '')
                || JSPatterns.jsLiteralExpression(JSPatterns.jsArgument(JSPatterns.jsReferenceExpression().withQualifiedName("Request"), 0)).accepts(jsLiteral)

                // {url: ''}
                || JSPatterns.jsLiteralExpression().inside(false, JSPatterns.jsExpression().withParent(JSPatterns.jsProperty().withName("url"))).accepts(jsLiteral)

                // allows basically to support all common use cases by resolve the argument "named argument"
                // foobar('foobar') => foobar(url)
                || JSPatterns.jsLiteralExpression().inside(false, jsArgumentOfName("url")).accepts(jsLiteral)
            ;
    }

    private static JSElementPattern.Capture<JSExpression> jsArgumentOfName(@NotNull final String name) {
        return new JSElementPattern.Capture<>(new InitialPatternCondition<>(JSExpression.class) {
            public boolean accepts(@Nullable Object o, @NotNull ProcessingContext context) {
                Intrinsics.checkNotNullParameter(context, "context");
                if (!(o instanceof JSExpression)) {
                    return false;
                } else {
                    PsiElement var10000 = ((JSExpression)o).getParent();
                    if (!(var10000 instanceof JSArgumentList)) {
                        var10000 = null;
                    }

                    JSArgumentList var4 = (JSArgumentList)var10000;
                    if (var4 != null) {
                        JSParameterItem var5 = JSResolveUtil.findParameterForUsedArgument((JSExpression)o, var4);
                        return Intrinsics.areEqual(var5 != null ? var5.getName() : null, name);
                    } else {
                        return false;
                    }
                }
            }
        });
    }
}
