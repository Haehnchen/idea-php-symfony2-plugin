package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerReference;
import org.jetbrains.annotations.NotNull;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpRouteReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar psiReferenceRegistrar) {
        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class).withLanguage(PhpLanguage.INSTANCE),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
                    if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContext() instanceof ParameterList)) {
                        return new PsiReference[0];
                    }
                    ParameterList parameterList = (ParameterList) psiElement.getContext();

                    if (parameterList == null || !(parameterList.getContext() instanceof MethodReference)) {
                        return new PsiReference[0];
                    }
                    MethodReference method = (MethodReference) parameterList.getContext();

                    Symfony2InterfacesUtil interfacesUtil = new Symfony2InterfacesUtil();
                    if (!interfacesUtil.isUrlGeneratorGenerateCall(method)) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{ new RouteReference((StringLiteralExpression) psiElement) };
                }
            }
        );

        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

                    if (!Symfony2ProjectComponent.isEnabled(psiElement) || !PsiElementUtils.isCallToWithParameter(psiElement, "\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "forward")) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{ new ControllerReference((StringLiteralExpression) psiElement) };
                }

            }

        );

        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {


                    if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
                        return new PsiReference[0];
                    }

                    ParameterList parameterList = PsiTreeUtil.getParentOfType(psiElement, ParameterList.class);
                    if (parameterList == null) {
                        return new PsiReference[0];
                    }

                    if(!(parameterList.getContext() instanceof MethodReference)) {
                        return new PsiReference[0];
                    }

                    ArrayCreationExpression arrayCreation = PsiTreeUtil.getParentOfType(psiElement, ArrayCreationExpression.class);
                    if(arrayCreation == null) {
                        return new PsiReference[0];
                    }

                    ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(arrayCreation);
                    if(currentIndex == null || currentIndex.getIndex() != 1) {
                        return new PsiReference[0];
                    }

                    MethodReference method = (MethodReference) parameterList.getContext();
                    Symfony2InterfacesUtil interfacesUtil = new Symfony2InterfacesUtil();
                    if (!interfacesUtil.isUrlGeneratorGenerateCall(method)) {
                        return new PsiReference[0];
                    }


                    ArrayCreationExpression arrayCreationExpression = PhpElementsUtil.getCompletableArrayCreationElement(psiElement);
                    if(arrayCreationExpression == null) {
                        return new PsiReference[0];
                    }

                    String routeName = PsiElementUtils.getMethodParameterAt(parameterList, 0);
                    if(routeName == null) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{ new RouteParameterReference((StringLiteralExpression) psiElement, routeName) };

                }

            }

        );

    }

}
