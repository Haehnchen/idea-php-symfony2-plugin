package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationNavigationCompletionContributor {
    public static class Completion extends CompletionContributor {
        public Completion() {
            // $this->translator->trans('<caret>', [], '<caret>');
            // new TranslatableMessage('<caret>', [], '<caret>');
            // t('<caret>', [], '<caret>');
            extend(CompletionType.BASIC, PlatformPatterns.or(
                PhpElementsUtil.getParameterInsideMethodReferencePattern(),
                PhpElementsUtil.getParameterInsideNewExpressionPattern(),
                PhpElementsUtil.getParameterInsideFunctionReferencePattern()
            ), new CompletionProvider<>() {
                @Override
                protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                    PsiElement psiElement = parameters.getOriginalPosition();
                    if (psiElement == null || !(psiElement.getParent() instanceof StringLiteralExpression stringLiteralExpression)) {
                        return;
                    }

                    Project project = parameters.getPosition().getProject();

                    visit(
                        stringLiteralExpression,
                        s -> result.addAllElements(TranslationUtil.getTranslationDomainLookupElements(project)),
                        (domain, s) -> result.addAllElements(TranslationUtil.getTranslationLookupElementsOnDomain(project, domain))
                    );
                }
            });
        }
    }

    public static class GotoDeclaration implements GotoDeclarationHandler {
        @Override
        public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement sourceElement, int offset, Editor editor) {
            if (sourceElement != null && sourceElement.getParent() instanceof StringLiteralExpression stringLiteralExpression && (
                PhpElementsUtil.getParameterInsideMethodReferencePattern().accepts(sourceElement)
                    || PhpElementsUtil.getParameterInsideNewExpressionPattern().accepts(sourceElement)
                    || PhpElementsUtil.getParameterInsideFunctionReferencePattern().accepts(sourceElement)
            )) {
                Collection<PsiElement> psiElements = new ArrayList<>();

                Project project = sourceElement.getProject();

                visit(
                    stringLiteralExpression,
                    s -> psiElements.addAll(TranslationUtil.getDomainPsiFiles(project, s.getContents())),
                    (domain, s) -> psiElements.addAll(Arrays.asList(TranslationUtil.getTranslationPsiElements(project, s.getContents(), domain)))
                );

                return psiElements.toArray(new PsiElement[0]);
            }

            return null;
        }
    }

    public static void visit(@NotNull StringLiteralExpression psiElement, @NotNull TranslationNavigationCompletionContributor.Domain domain, @NotNull TranslationNavigationCompletionContributor.Key key) {
        if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContext() instanceof ParameterList parameterList)) {
            return;
        }

        if (!(parameterList.getContext() instanceof ParameterListOwner parameterListOwner)) {
            return;
        };

        if (!TranslationUtil.isTranslationReference(parameterListOwner)) {
            return;
        }

        int domainParameter = PhpTranslationDomainInspection.getDomainParameter(parameterListOwner);

        if (PsiElementUtils.isCurrentParameter(psiElement, "domain", domainParameter)) {
            domain.accept(psiElement);
            return;
        }

        if (PsiElementUtils.isCurrentParameter(psiElement, "id", 0)) {
            PsiElement domainPsi = parameterList.getParameter("domain", domainParameter);

            String domainName = "messages";
            if (domainPsi != null) {
                String stringValue = PhpElementsUtil.getStringValue(domainPsi);
                if (stringValue != null) {
                    domainName = stringValue;
                }
            }

            key.accept(domainName, psiElement);
        }
    }

    private interface Domain {
        void accept(@NotNull StringLiteralExpression stringLiteralExpression);
    }

    private interface Key {
        void accept(@NotNull String domain, @NotNull StringLiteralExpression stringLiteralExpression);
    }
}
