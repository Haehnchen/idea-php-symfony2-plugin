package de.espend.idea.php.drupal.references;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.ClassReference;
import com.jetbrains.php.lang.psi.elements.NewExpression;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import de.espend.idea.php.drupal.DrupalProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteParameterReference;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteReference;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpRouteReferenceContributor extends PsiReferenceContributor {

    final private static MethodMatcher.CallToSignature[] GENERATOR_SIGNATURES = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Drupal\\Core\\Routing\\UrlGeneratorInterface", "getPathFromRoute"), // <- <@TODO: remove: pre Drupal8 beta
        new MethodMatcher.CallToSignature("\\Drupal\\Core\\Routing\\UrlGeneratorInterface", "generateFromRoute"), // <- <@TODO: remove: pre Drupal8 beta
        new MethodMatcher.CallToSignature("\\Drupal\\Core\\Routing\\UrlGenerator", "getPathFromRoute"),
        new MethodMatcher.CallToSignature("\\Drupal\\Core\\Routing\\UrlGenerator", "generateFromRoute"),
        new MethodMatcher.CallToSignature("\\Drupal\\Core\\Url", "fromRoute"),
        new MethodMatcher.CallToSignature("\\Drupal\\Core\\Form\\FormStateInterface", "setRedirect"),
        new MethodMatcher.CallToSignature("\\Drupal", "url"),
    };

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar psiReferenceRegistrar) {

        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class).withLanguage(PhpLanguage.INSTANCE),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

                    if(!DrupalProjectComponent.isEnabled(psiElement)) {
                        return new PsiReference[0];
                    }

                    if (MethodMatcher.getMatchedSignatureWithDepth(psiElement, GENERATOR_SIGNATURES) == null) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{ new RouteReference((StringLiteralExpression) psiElement) };
                }
            }
        );

        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class).withLanguage(PhpLanguage.INSTANCE),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

                    if(!DrupalProjectComponent.isEnabled(psiElement)) {
                        return new PsiReference[0];
                    }

                    if(psiElement instanceof StringLiteralExpression) {
                        PsiElement parameterList = psiElement.getParent();
                        if(parameterList instanceof ParameterList) {
                            PsiElement newExpression = parameterList.getParent();
                            if(newExpression instanceof NewExpression) {
                                ClassReference classReference = ((NewExpression) newExpression).getClassReference();
                                if(classReference != null && "Url".equals(classReference.getName())) {
                                    String fqn = classReference.getFQN();
                                    if(fqn != null && fqn.equals("\\Drupal\\Core\\Url")) {
                                        return new PsiReference[]{ new RouteReference((StringLiteralExpression) psiElement) };
                                    }
                                }
                            }
                        }
                    }

                    return new PsiReference[0];
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
