package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class ServiceReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar psiReferenceRegistrar) {
        psiReferenceRegistrar.registerReferenceProvider(PlatformPatterns.psiElement(StringLiteralExpression.class), new PsiReferenceProvider() {
            @NotNull
            @Override
            public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
                ParameterList parameterList = (ParameterList) psiElement.getContext();
                if (!(parameterList instanceof ParameterList)) {
                    return new PsiReference[0];
                }

                MethodReference method = (MethodReference) parameterList.getContext();
                if (!(method instanceof MethodReference)) {
                    return new PsiReference[0];
                }

                if (!ContainerGetHelper.isContainerGetCall(method)) {
                    return new PsiReference[0];
                }

                return new PsiReference[]{ new ServiceReference((StringLiteralExpression) psiElement) };
            }
        });
    }
}
