package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.NewExpression;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar psiReferenceRegistrar) {

        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
                    if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContext() instanceof ParameterList)) {
                        return new PsiReference[0];
                    }

                    ParameterList parameterList = (ParameterList) psiElement.getContext();
                    PsiElement methodReferenceOrNewExpression = parameterList.getContext();

                    if (!(
                        (methodReferenceOrNewExpression instanceof MethodReference && PhpElementsUtil.isMethodReferenceInstanceOf((MethodReference) methodReferenceOrNewExpression, TranslationUtil.PHP_TRANSLATION_SIGNATURES)) ||
                            (methodReferenceOrNewExpression instanceof NewExpression && PhpElementsUtil.isNewExpressionPhpClassWithInstance((NewExpression) methodReferenceOrNewExpression, TranslationUtil.PHP_TRANSLATION_TRANSLATABLE_MESSAGE)))
                    ) {
                        return new PsiReference[0];
                    }

                    int domainParameter = PhpTranslationDomainInspection.getDomainParameter(methodReferenceOrNewExpression);

                    if (PsiElementUtils.isCurrentParameter(psiElement, "domain", domainParameter)) {
                        return new PsiReference[]{ new TranslationDomainReference((StringLiteralExpression) psiElement) };
                    }

                    if (PsiElementUtils.isCurrentParameter(psiElement, "id", 0)) {
                        PsiElement domainPsi = parameterList.getParameter("domain", domainParameter);

                        String domain = "messages";
                        if (domainPsi != null) {
                            String stringValue = PhpElementsUtil.getStringValue(domainPsi);
                            if (stringValue != null) {
                                domain = stringValue;
                            }
                        }

                        return new PsiReference[]{ new TranslationReference((StringLiteralExpression) psiElement, domain) };
                    }

                    return new PsiReference[0];
                }

                @Override
                public boolean acceptsTarget(@NotNull PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );
    }
}
