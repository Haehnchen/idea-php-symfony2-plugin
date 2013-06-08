package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
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
                    public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
                        if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContext() instanceof ParameterList)) {
                            return new PsiReference[0];
                        }

                        ParameterList parameterList = (ParameterList) psiElement.getContext();

                        if (!(parameterList.getContext() instanceof MethodReference)) {
                            return new PsiReference[0];
                        }

                        MethodReference method = (MethodReference) parameterList.getContext();
                        Symfony2InterfacesUtil interfacesUtil = new Symfony2InterfacesUtil();
                        if (!interfacesUtil.isTranslatorCall(method)) {
                            return new PsiReference[0];
                        }

                        // only use parameter, 1 = string_id and 3 = domain
                        ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(psiElement);
                        if(currentIndex == null || currentIndex.getIndex() == 1 || currentIndex.getIndex() > 2) {
                            return new PsiReference[0];
                        }

                        // pipe domain if it is set
                        String domain = PsiElementUtils.getMethodParameterAt(parameterList, 2);
                        return new PsiReference[]{ new TranslationReference((StringLiteralExpression) psiElement, domain, currentIndex) };
                    }

                }

        );
    }

}
