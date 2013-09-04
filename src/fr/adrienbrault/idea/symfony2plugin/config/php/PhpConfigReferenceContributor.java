package fr.adrienbrault.idea.symfony2plugin.config.php;


import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.ClassReference;
import com.jetbrains.php.lang.psi.elements.NewExpression;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.PhpClassReference;
import fr.adrienbrault.idea.symfony2plugin.config.component.ParameterReference;
import fr.adrienbrault.idea.symfony2plugin.config.dic.EventDispatcherEventReference;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceReference;
import fr.adrienbrault.idea.symfony2plugin.dic.TagReference;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpStringLiteralExpressionReference;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;


/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpConfigReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar psiReferenceRegistrar) {

        psiReferenceRegistrar.registerReferenceProvider(PhpElementsUtil.methodWithFirstStringPattern(), new PhpStringLiteralExpressionReference(ParameterReference.class)
            .addCall("\\Symfony\\Component\\DependencyInjection\\ContainerInterface", "hasParameter")
            .addCall("\\Symfony\\Component\\DependencyInjection\\ContainerInterface", "getParameter")
        );

        psiReferenceRegistrar.registerReferenceProvider(PhpElementsUtil.methodWithFirstStringPattern(), new PhpStringLiteralExpressionReference(ServiceReference.class)
            .addCall("\\Symfony\\Component\\DependencyInjection\\ContainerInterface", "has")
            .addCall("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "has")
            .addCall("\\Symfony\\Component\\DependencyInjection\\ContainerBuilder", "hasDefinition")
            .addCall("\\Symfony\\Component\\DependencyInjection\\ContainerBuilder", "getDefinition")
            .addCall("\\Symfony\\Component\\DependencyInjection\\ContainerBuilder", "setAlias", 1)
            .addCall("\\Symfony\\Component\\DependencyInjection\\ContainerBuilder", "findDefinition")
        );

        psiReferenceRegistrar.registerReferenceProvider(PhpElementsUtil.methodWithFirstStringPattern(), new PhpStringLiteralExpressionReference(TagReference.class)
            .addCall("\\Symfony\\Component\\DependencyInjection\\ContainerBuilder", "findTaggedServiceIds")
        );

        psiReferenceRegistrar.registerReferenceProvider(PhpElementsUtil.methodWithFirstStringPattern(), new PhpStringLiteralExpressionReference(EventDispatcherEventReference.class)
            .addCall("\\Symfony\\Component\\EventDispatcher\\EventDispatcherInterface", "dispatch")
        );

        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class).withLanguage(PhpLanguage.INSTANCE),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

                    if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                        return new PsiReference[0];
                    }

                    if (!phpStringLiteralExpressionClassReference("\\Symfony\\Component\\DependencyInjection\\Reference", 0, psiElement) &&
                        !phpStringLiteralExpressionClassReference("\\Symfony\\Component\\DependencyInjection\\Alias", 0, psiElement)
                    ) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{ new ServiceReference((StringLiteralExpression) psiElement) };
                }
            }
        );

        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class).withLanguage(PhpLanguage.INSTANCE),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

                    if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                        return new PsiReference[0];
                    }

                    if (!phpStringLiteralExpressionClassReference("\\Symfony\\Component\\DependencyInjection\\Definition", 0, psiElement)) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{ new PhpClassReference((StringLiteralExpression) psiElement, true) };
                }
            }
        );

    }

    private static boolean phpStringLiteralExpressionClassReference(String signature, int index, PsiElement psiElement) {

        if (!(psiElement.getContext() instanceof ParameterList)) {
            return false;
        }

        ParameterList parameterList = (ParameterList) psiElement.getContext();
        if (parameterList == null || !(parameterList.getContext() instanceof NewExpression)) {
            return false;
        }

        if(PsiElementUtils.getParameterIndexValue(psiElement) != index) {
            return false;
        }

        NewExpression newExpression = (NewExpression) parameterList.getContext();
        ClassReference classReference = newExpression.getClassReference();
        if(classReference == null) {
            return false;
        }

        return classReference.getSignature().equals("#C" + signature);
    }

}
