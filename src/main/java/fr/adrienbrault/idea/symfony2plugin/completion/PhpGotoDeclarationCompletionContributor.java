package fr.adrienbrault.idea.symfony2plugin.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import icons.SymfonyIcons;
import kotlin.Pair;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpGotoDeclarationCompletionContributor {
    public static class Completion extends CompletionContributor {
        public Completion() {
            // $httpClient->request('', '', ['<caret>' => '']);
            extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new HttpClientCompletionProvider());
        }
    }

    public static class GotoDeclaration implements GotoDeclarationHandler {
        @Override
        public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement psiElement, int offset, Editor editor) {
            if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                return new PsiElement[0];
            }

            PsiElement context = psiElement.getParent();
            Collection<PsiElement> psiElements = new ArrayList<>();
            if (context instanceof StringLiteralExpression) {
                String contents = ((StringLiteralExpression) context).getContents();
                if (StringUtils.isNotBlank(contents)) {
                    // $httpClient->request('', '', ['<caret>' => '']);
                    if (isHttpClientOptionArgument(psiElement)) {
                        consumeHttpClientOptions(psiElement.getProject(), pair -> {
                            if (pair.getFirst().equals(contents)) {
                                psiElements.add(pair.getSecond().getFirst());
                            }
                        });
                    }
                }
            }

            return psiElements.toArray(new PsiElement[0]);
        }
    }

    private static class HttpClientCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

            PsiElement psiElement = completionParameters.getOriginalPosition();
            if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContext() instanceof StringLiteralExpression)) {
                return;
            }

            if (!isHttpClientOptionArgument(psiElement)) {
                return;
            }

            consumeHttpClientOptions(psiElement.getProject(), pair -> {
                LookupElementBuilder element = LookupElementBuilder.create(pair.getFirst())
                    .withIcon(SymfonyIcons.Symfony);

                PsiElement value = pair.getSecond().getSecond();
                if (value instanceof PhpTypedElement) {
                    String s = ((PhpTypedElement) value).getType().toString();
                    if (StringUtils.isNotBlank(s)) {
                        element = element.withTypeText(s, true);
                    }
                }

                completionResultSet.addElement(element);
            });
        }
    }

    private static void consumeHttpClientOptions(@NotNull Project project, @NotNull Consumer<Pair<String, Pair<PsiElement, PsiElement>>> consumer) {
        for (PhpClass phpClass : PhpElementsUtil.getClassesInterface(project, "\\Symfony\\Contracts\\HttpClient\\HttpClientInterface")) {
            Field optionsDefaults = phpClass.findFieldByName("OPTIONS_DEFAULTS", true);
            if (optionsDefaults != null && optionsDefaults.isConstant()) {
                PsiElement defaultValue = optionsDefaults.getDefaultValue();
                if (defaultValue instanceof ArrayCreationExpression) {
                    for (Map.Entry<String, Pair<PsiElement, PsiElement>> entry : PhpElementsUtil.getArrayKeyValueMapWithKeyAndValueElement((ArrayCreationExpression) defaultValue).entrySet()) {
                        consumer.accept(new Pair<>(entry.getKey(), entry.getValue()));
                    }
                }
            }
        }
    }

    private static boolean isHttpClientOptionArgument(@NotNull PsiElement psiElement) {
        ArrayCreationExpression arrayCreationExpression = PhpElementsUtil.getCompletableArrayCreationElement(psiElement.getParent());
        if (arrayCreationExpression == null) {
            return false;
        }

        MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(arrayCreationExpression, 2)
            .withSignature("\\Symfony\\Contracts\\HttpClient\\HttpClientInterface", "request")
            .match();

        if (methodMatchParameter == null) {
            methodMatchParameter = new MethodMatcher.StringParameterMatcher(arrayCreationExpression, 0)
                .withSignature("\\Symfony\\Contracts\\HttpClient\\HttpClientInterface", "withOptions")
                .match();
        }

        return methodMatchParameter != null;
    }
}
