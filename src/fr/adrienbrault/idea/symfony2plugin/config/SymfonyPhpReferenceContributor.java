package fr.adrienbrault.idea.symfony2plugin.config;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.ConstraintPropertyReference;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceReference;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityReference;
import fr.adrienbrault.idea.symfony2plugin.doctrine.ModelFieldReference;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineTypes;
import fr.adrienbrault.idea.symfony2plugin.templating.TemplateReference;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyPhpReferenceContributor extends PsiReferenceContributor {

    private static MethodMatcher.CallToSignature[] CONTAINER_SIGNATURES = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "get"),
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\DependencyInjection\\ContainerInterface", "get"),
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\DependencyInjection\\ContainerInterface", "has"),
        new MethodMatcher.CallToSignature("\\Psr\\Container\\ContainerInterface", "get"),
        new MethodMatcher.CallToSignature("\\Psr\\Container\\ContainerInterface", "has"),
    };

    public static MethodMatcher.CallToSignature[] REPOSITORY_SIGNATURES = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Doctrine\\Common\\Persistence\\ManagerRegistry", "getRepository"),
        new MethodMatcher.CallToSignature("\\Doctrine\\Common\\Persistence\\ObjectManager", "getRepository"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\EntityManager", "getReference"),
        new MethodMatcher.CallToSignature("\\Doctrine\\Common\\Persistence\\ManagerRegistry", "getManagerForClass"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "update"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "delete"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\QueryBuilder", "from"),

        // doctrine 2.5 methods
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Cache", "getEntityCacheRegion"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Cache", "containsEntity"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Cache", "evictEntity"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Cache", "evictEntityRegion"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Cache", "containsCollection"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Cache", "evictCollection"),
        new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Cache", "evictCollectionRegion"),
    };

    public static MethodMatcher.CallToSignature[] TEMPLATE_SIGNATURES = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Templating\\EngineInterface", "render"),
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Templating\\StreamingEngineInterface", "stream"),
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Templating\\EngineInterface", "renderResponse"),
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "render"),
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "renderView"),
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "stream"),
        new MethodMatcher.CallToSignature("\\Twig_Environment", "render"),
        new MethodMatcher.CallToSignature("\\Twig_Environment", "loadTemplate"),
        new MethodMatcher.CallToSignature("\\Twig_Environment", "getTemplateClass"),
        new MethodMatcher.CallToSignature("\\Twig_Environment", "display"),
        new MethodMatcher.CallToSignature("\\Twig_Environment", "isTemplateFresh"),
        new MethodMatcher.CallToSignature("\\Twig_Environment", "resolveTemplate"), // @TODO: also "is_array($names)"
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

                    return new PsiReference[]{ new ServiceReference((StringLiteralExpression) psiElement, false) };

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
            // @TODO: implement global pattern for array parameters
            PlatformPatterns.psiElement(StringLiteralExpression.class).withParent(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PhpElementTypes.ARRAY_VALUE),
                    PlatformPatterns.psiElement(PhpElementTypes.ARRAY_KEY)
                )
            ).inside(PlatformPatterns.psiElement(ParameterList.class)),
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
                    if(phpClasses.size() == 0) {
                        return new PsiReference[0];
                    }

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

                    if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                        return new PsiReference[0];
                    }

                    PhpClass phpClass = getClassArrayCreationParameter(psiElement, 0, "\\Symfony\\Component\\Validator\\Constraint");
                    if(phpClass != null) {
                        return new PsiReference[] { new ConstraintPropertyReference((StringLiteralExpression) psiElement, phpClass)};
                    }

                    return new PsiReference[0];
                }

                private PhpClass getClassArrayCreationParameter(PsiElement psiElement, int parameterIndex, String instance) {
                    ArrayCreationExpression arrayCreationExpression = PhpElementsUtil.getCompletableArrayCreationElement(psiElement);
                    if (arrayCreationExpression != null) {

                        PsiElement parameterList = arrayCreationExpression.getContext();
                        if (parameterList instanceof ParameterList) {
                            PsiElement methodParameters[] = ((ParameterList) parameterList).getParameters();
                            if (!(methodParameters.length < parameterIndex)) {
                                PsiElement newExpression = parameterList.getContext();
                                if (newExpression instanceof NewExpression) {
                                    ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(arrayCreationExpression);
                                    if (currentIndex != null && currentIndex.getIndex() == parameterIndex) {
                                        // @TODO: getNewExpressionPhpClassWithInstance
                                        ClassReference classReference = ((NewExpression) newExpression).getClassReference();
                                        if(classReference != null) {
                                            String fqn = classReference.getFQN();
                                            if(fqn != null) {
                                                PhpClass phpClass = PhpElementsUtil.getClass(psiElement.getProject(), fqn);
                                                if(phpClass != null) {
                                                    if(PhpElementsUtil.isInstanceOf(phpClass, instance)) {
                                                        return phpClass;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    return null;
                }
            }
        );
    }
}
