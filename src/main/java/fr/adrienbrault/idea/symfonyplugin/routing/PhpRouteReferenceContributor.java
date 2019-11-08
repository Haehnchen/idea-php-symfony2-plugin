package fr.adrienbrault.idea.symfonyplugin.routing;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfonyplugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfonyplugin.util.controller.ControllerReference;
import org.jetbrains.annotations.NotNull;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpRouteReferenceContributor extends PsiReferenceContributor {

    public static MethodMatcher.CallToSignature[] GENERATOR_SIGNATURES = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "generateUrl"),
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "redirectToRoute"),

        // Symfony 3.3 / 3.4
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\ControllerTrait", "generateUrl"),
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\ControllerTrait", "redirectToRoute"),

        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface", "generate"),

    };

    private static MethodMatcher.CallToSignature[] FORWARD_SIGNATURES = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "forward"),

        // Symfony 3.3 / 3.4
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\ControllerTrait", "forward")
    };

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar psiReferenceRegistrar) {
        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class).withLanguage(PhpLanguage.INSTANCE),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

                    if (MethodMatcher.getMatchedSignatureWithDepth(psiElement, GENERATOR_SIGNATURES) == null) {
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

                    if (MethodMatcher.getMatchedSignatureWithDepth(psiElement, FORWARD_SIGNATURES) == null) {
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

                    MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.ArrayParameterMatcher(psiElement, 1)
                        .withSignature(GENERATOR_SIGNATURES)
                        .match();

                    if(methodMatchParameter == null) {
                        return new PsiReference[0];
                    }

                    String routeName = PsiElementUtils.getMethodParameterAt(methodMatchParameter.getMethodReference(), 0);
                    if(routeName == null) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{ new RouteParameterReference((StringLiteralExpression) psiElement, routeName) };

                }

            }

        );

    }

}
