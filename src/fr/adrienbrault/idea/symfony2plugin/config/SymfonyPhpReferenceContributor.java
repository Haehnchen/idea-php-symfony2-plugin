package fr.adrienbrault.idea.symfony2plugin.config;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.dic.MapClassReference;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceReference;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityReference;
import fr.adrienbrault.idea.symfony2plugin.doctrine.ModelFieldReference;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineTypes;
import fr.adrienbrault.idea.symfony2plugin.templating.TemplateReference;
import fr.adrienbrault.idea.symfony2plugin.util.CommandUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyPhpReferenceContributor extends PsiReferenceContributor {

    private static MethodMatcher.CallToSignature[] CONTAINER_SIGNATURES = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\DependencyInjection\\ContainerInterface", "get"),
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "get"),
    };

    public static MethodMatcher.CallToSignature[] REPOSITORY_SIGNATURES = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Doctrine\\Common\\Persistence\\ManagerRegistry", "getRepository"),
        new MethodMatcher.CallToSignature("\\Doctrine\\Common\\Persistence\\ObjectManager", "getRepository"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\EntityManager", "getReference"),
        new MethodMatcher.CallToSignature("\\Doctrine\\Common\\Persistence\\ManagerRegistry", "getManagerForClass"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "update"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "delete"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "from"),
    };

    public static MethodMatcher.CallToSignature[] TEMPLATE_SIGNATURES = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Templating\\EngineInterface", "render"),
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Templating\\StreamingEngineInterface", "stream"),
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Templating\\EngineInterface", "renderResponse"),
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "render"),
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "renderView"),
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "stream"),
    };

    public static MethodMatcher.CallToSignature[] CONSOLE_HELP_SET = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Console\\Helper\\HelperSet", "get"),
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Console\\Helper\\HelperSet", "has"),
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Console\\Command\\Command", "getHelper"),
    };

    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar psiReferenceRegistrar) {

        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class).withLanguage(PhpLanguage.INSTANCE),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

                    if (MethodMatcher.getMatchedSignatureWithDepth(psiElement, CONTAINER_SIGNATURES) == null) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{ new ServiceReference((StringLiteralExpression) psiElement, true, false) };

                }
            }
        );
        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

                    if (MethodMatcher.getMatchedSignatureWithDepth(psiElement, TEMPLATE_SIGNATURES) == null) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{ new TemplateReference((StringLiteralExpression) psiElement) };
                }
            }
        );

        psiReferenceRegistrar.registerReferenceProvider(
                PlatformPatterns.psiElement(StringLiteralExpression.class),
                new PsiReferenceProvider() {
                    @NotNull
                    @Override
                    public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

                        MethodMatcher.MethodMatchParameter methodMatchParameter = MethodMatcher.getMatchedSignatureWithDepth(psiElement, REPOSITORY_SIGNATURES);
                        if (methodMatchParameter == null) {
                            return new PsiReference[0];
                        }

                        DoctrineTypes.Manager manager = EntityHelper.getManager(methodMatchParameter.getMethodReference());
                        if(manager != null) {
                            return new PsiReference[]{ new EntityReference((StringLiteralExpression) psiElement, manager) };
                        }

                        return new PsiReference[]{ new EntityReference((StringLiteralExpression) psiElement) };
                    }
                }
        );

        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

                    MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement, 0)
                        .withSignature("\\Doctrine\\Common\\Persistence\\ObjectManager", "find")
                        .match();

                    if(methodMatchParameter == null) {
                        return new PsiReference[0];
                    }

                    DoctrineTypes.Manager manager = EntityHelper.getManager(methodMatchParameter.getMethodReference());
                    if(manager != null) {
                        return new PsiReference[]{ new EntityReference((StringLiteralExpression) psiElement, manager) };
                    }

                    return new PsiReference[]{ new EntityReference((StringLiteralExpression) psiElement) };
                }
            }
        );

        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

                    MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.ArrayParameterMatcher(psiElement, 0)
                        .withSignature("\\Doctrine\\Common\\Persistence\\ObjectRepository", "findOneBy")
                        .withSignature("\\Doctrine\\Common\\Persistence\\ObjectRepository", "findBy")
                        .match();

                    if(methodMatchParameter == null) {
                        return new PsiReference[0];
                    }

                    Collection<PhpClass> phpClasses = PhpElementsUtil.getClassFromPhpTypeSetArrayClean(psiElement.getProject(), methodMatchParameter.getMethodReference().getType().getTypes());
                    return new PsiReference[]{ new ModelFieldReference((StringLiteralExpression) psiElement, phpClasses)};
                }
            }
        );

        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

                    if (MethodMatcher.getMatchedSignatureWithDepth(psiElement, CONSOLE_HELP_SET) == null) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{ new MapClassReference((StringLiteralExpression) psiElement, CommandUtil.getCommandHelper(psiElement.getProject()))};
                }
            }
        );

    }

}
