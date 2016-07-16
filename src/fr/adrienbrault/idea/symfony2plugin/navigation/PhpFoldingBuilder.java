package fr.adrienbrault.idea.symfony2plugin.navigation;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.SymfonyPhpReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.routing.PhpRouteReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


public class PhpFoldingBuilder extends FoldingBuilderEx {

    @NotNull
    @Override
    public FoldingDescriptor[] buildFoldRegions(@NotNull PsiElement psiElement, @NotNull Document document, boolean b) {

        if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement instanceof PhpFile)) {
            return new FoldingDescriptor[0];
        }

        boolean codeFoldingPhpRoute = Settings.getInstance(psiElement.getProject()).codeFoldingPhpRoute;
        boolean codeFoldingPhpModel = Settings.getInstance(psiElement.getProject()).codeFoldingPhpModel;
        boolean codeFoldingPhpTemplate = Settings.getInstance(psiElement.getProject()).codeFoldingPhpTemplate;

        // we dont need to do anything
        if(!codeFoldingPhpRoute && !codeFoldingPhpModel && !codeFoldingPhpTemplate) {
            return new FoldingDescriptor[0];
        }

        List<FoldingDescriptor> descriptors = new ArrayList<>();

        Collection<StringLiteralExpression> stringLiteralExpressiones = PsiTreeUtil.findChildrenOfType(psiElement, StringLiteralExpression.class);
        for(StringLiteralExpression stringLiteralExpression: stringLiteralExpressiones) {

            if(codeFoldingPhpModel) {
                attachModelShortcuts(descriptors, stringLiteralExpression);
            }

            if(codeFoldingPhpTemplate) {
                attachTemplateShortcuts(descriptors, stringLiteralExpression);
            }

        }

        // strip ".[php|html].twig"
        if(codeFoldingPhpRoute) {
            attachRouteShortcuts(descriptors, stringLiteralExpressiones);
        }

        return descriptors.toArray(new FoldingDescriptor[descriptors.size()]);
    }


    private void attachRouteShortcuts(List<FoldingDescriptor> descriptors, Collection<StringLiteralExpression> stringLiteralExpressions) {

        Map<String,Route> routes = null;

        for(StringLiteralExpression stringLiteralExpression: stringLiteralExpressions) {

            if (MethodMatcher.getMatchedSignatureWithDepth(stringLiteralExpression, PhpRouteReferenceContributor.GENERATOR_SIGNATURES) != null) {

                // cache routes if we need them
                if(routes == null) {
                    routes = RouteHelper.getAllRoutes(stringLiteralExpression.getProject());
                }

                String contents = stringLiteralExpression.getContents();
                if(contents.length() > 0 && routes.containsKey(contents)) {
                    final Route route = routes.get(contents);

                    final String url = RouteHelper.getRouteUrl(route);
                    if(url != null) {
                        descriptors.add(new FoldingDescriptor(stringLiteralExpression.getNode(),
                            new TextRange(stringLiteralExpression.getTextRange().getStartOffset() + 1, stringLiteralExpression.getTextRange().getEndOffset() - 1)) {
                            @Nullable
                            @Override
                            public String getPlaceholderText() {
                                return url;
                            }
                        });
                    }
                }
            }

        }

    }

    private void attachModelShortcuts(List<FoldingDescriptor> descriptors, final StringLiteralExpression stringLiteralExpression) {

        if (MethodMatcher.getMatchedSignatureWithDepth(stringLiteralExpression, SymfonyPhpReferenceContributor.REPOSITORY_SIGNATURES) == null) {
            return;
        }

        String content = stringLiteralExpression.getContents();

        for(String lastChar: new String[] {":", "\\"}) {
            if(content.contains(lastChar)) {
                final String replace = content.substring(content.lastIndexOf(lastChar) + 1);
                if(replace.length() > 0) {
                    descriptors.add(new FoldingDescriptor(stringLiteralExpression.getNode(),
                        new TextRange(stringLiteralExpression.getTextRange().getStartOffset() + 1, stringLiteralExpression.getTextRange().getEndOffset() - 1)) {
                        @Nullable
                        @Override
                        public String getPlaceholderText() {
                            return replace;
                        }
                    });
                }

                return;
            }
        }

    }

    private void attachTemplateShortcuts(List<FoldingDescriptor> descriptors, final StringLiteralExpression stringLiteralExpression) {

        if (MethodMatcher.getMatchedSignatureWithDepth(stringLiteralExpression, SymfonyPhpReferenceContributor.TEMPLATE_SIGNATURES) == null) {
            return;
        }

        String content = stringLiteralExpression.getContents();

        String templateShortcutName = TwigUtil.getFoldingTemplateName(content);
        if(templateShortcutName == null) {
            return;
        }

        final String finalTemplateShortcutName = templateShortcutName;
        descriptors.add(new FoldingDescriptor(stringLiteralExpression.getNode(),
            new TextRange(stringLiteralExpression.getTextRange().getStartOffset() + 1, stringLiteralExpression.getTextRange().getEndOffset() - 1)) {
            @Nullable
            @Override
            public String getPlaceholderText() {
                return finalTemplateShortcutName;
            }
        });

    }

    @Nullable
    @Override
    public String getPlaceholderText(@NotNull ASTNode astNode) {
        return "...";
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode astNode) {
        return true;
    }
}
