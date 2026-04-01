package fr.adrienbrault.idea.symfony2plugin.config.php;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.ClassPublicMethodReference;
import fr.adrienbrault.idea.symfony2plugin.config.PhpClassReference;
import fr.adrienbrault.idea.symfony2plugin.config.dic.EventDispatcherEventReference;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceIndexedReference;
import fr.adrienbrault.idea.symfony2plugin.dic.AbstractServiceReference;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceReference;
import fr.adrienbrault.idea.symfony2plugin.dic.TagReference;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpStringLiteralExpressionReference;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpConfigReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar psiReferenceRegistrar) {
        psiReferenceRegistrar.registerReferenceProvider(PhpElementsUtil.getMethodWithFirstStringOrNamedArgumentPattern(), new PhpStringLiteralExpressionReference(ServiceReference.class)
            .addCall("\\Symfony\\Component\\DependencyInjection\\ContainerInterface", "has")
            .addCall("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "has")
            .addCall("\\Psr\\Container\\ContainerInterface", "has")

            // Symfony 3.3 / 3.4
            .addCall("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\ControllerTrait", "has")

            // Symfony 4
            .addCall("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController", "has")

            .addCall("\\Symfony\\Component\\DependencyInjection\\ParameterBag\\ContainerBagInterface", "has")
        );

        psiReferenceRegistrar.registerReferenceProvider(PhpElementsUtil.getMethodWithFirstStringOrNamedArgumentPattern(), new PhpStringLiteralExpressionReference(ServiceIndexedReference.class)
            .addCall("\\Symfony\\Component\\DependencyInjection\\ContainerBuilder", "hasDefinition")
            .addCall("\\Symfony\\Component\\DependencyInjection\\ContainerBuilder", "getDefinition")
            .addCall("\\Symfony\\Component\\DependencyInjection\\ContainerBuilder", "findDefinition")
            .addCall("\\Symfony\\Component\\DependencyInjection\\ContainerBuilder", "removeDefinition")
            .addCall("\\Symfony\\Component\\DependencyInjection\\ContainerBuilder", "removeAlias")

            .addCall("\\Symfony\\Component\\DependencyInjection\\Loader\\Configurator\\ServicesConfigurator", "get")
        );

        psiReferenceRegistrar.registerReferenceProvider(PhpElementsUtil.getMethodParameterListStringPattern(), new PhpStringLiteralExpressionReference(ServiceIndexedReference.class)
            .addCall("\\Symfony\\Component\\DependencyInjection\\ContainerBuilder", "setAlias", 1)
            .addCall("\\Symfony\\Component\\DependencyInjection\\Loader\\Configurator\\ServicesConfigurator", "alias", 1)
            .addCall("\\Symfony\\Component\\DependencyInjection\\Loader\\Configurator\\AbstractServiceConfigurator", "alias", 1)
        );

        psiReferenceRegistrar.registerReferenceProvider(PhpElementsUtil.getMethodWithFirstStringOrNamedArgumentPattern(), new PhpStringLiteralExpressionReference(TagReference.class)
            .addCall("\\Symfony\\Component\\DependencyInjection\\ContainerBuilder", "findTaggedServiceIds")
            .addCall("\\Symfony\\Component\\DependencyInjection\\Definition", "addTag")
            .addCall("\\Symfony\\Component\\DependencyInjection\\Definition", "hasTag")
            .addCall("\\Symfony\\Component\\DependencyInjection\\Definition", "clearTag")

            .addCall("\\Symfony\\Component\\DependencyInjection\\Loader\\Configurator\\Traits\\TagTrait", "tag")
        );

        psiReferenceRegistrar.registerReferenceProvider(PhpElementsUtil.getMethodWithFirstStringOrNamedArgumentPattern(), new PhpStringLiteralExpressionReference(EventDispatcherEventReference.class)
            .addCall("\\Symfony\\Component\\EventDispatcher\\EventDispatcherInterface", "dispatch")
        );

        // #[AsEventListener(event: '<caret>')]
        psiReferenceRegistrar.registerReferenceProvider(
            PhpElementsUtil.getAttributeNamedArgumentStringLiteralPattern("\\Symfony\\Component\\EventDispatcher\\Attribute\\AsEventListener", "event"),
            new PsiReferenceProvider() {
                @Override
                public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
                    if (!Symfony2ProjectComponent.isEnabled(element) || !(element instanceof StringLiteralExpression stringLiteralExpression)) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{
                        new EventDispatcherEventReference(element, stringLiteralExpression.getContents())
                    };
                }

                public boolean acceptsTarget(@NotNull PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );

        psiReferenceRegistrar.registerReferenceProvider(
            PhpElementsUtil.getMethodWithFirstStringOrNamedArgumentPattern(),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
                    if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                        return new PsiReference[0];
                    }

                    if (!phpStringLiteralExpressionClassReference("\\Symfony\\Component\\DependencyInjection\\Reference", 0, psiElement) &&
                        !phpStringLiteralExpressionClassReference("\\Symfony\\Component\\DependencyInjection\\Alias", 0, psiElement) &&
                        !phpStringLiteralExpressionClassReference("\\Symfony\\Component\\DependencyInjection\\DefinitionDecorator", 0, psiElement)
                    ) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{ new ServiceReference((StringLiteralExpression) psiElement, true) };
                }

                @Override
                public boolean acceptsTarget(@NotNull PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );

        // service('<caret>'), ref('<caret>') (ref is deprecated)
        psiReferenceRegistrar.registerReferenceProvider(
            PhpElementsUtil.getFunctionWithFirstStringPattern("service", "ref"),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
                    if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{ new ServiceReference((StringLiteralExpression) psiElement, true) };
                }

                @Override
                public boolean acceptsTarget(@NotNull PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );

        psiReferenceRegistrar.registerReferenceProvider(
            PhpElementsUtil.getMethodWithFirstStringOrNamedArgumentPattern(),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
                    if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                        return new PsiReference[0];
                    }

                    if (!phpStringLiteralExpressionClassReference("\\Symfony\\Component\\DependencyInjection\\Definition", 0, psiElement)) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{ new PhpClassReference((StringLiteralExpression) psiElement, true) };
                }

                @Override
                public boolean acceptsTarget(@NotNull PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );

        // array key in return statement
        // \Symfony\Component\EventDispatcher\EventSubscriberInterface::getSubscribedEvents
        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
                    if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                        return new PsiReference[0];
                    }

                    ArrayCreationExpression arrayCreationExpression = PhpElementsUtil.getCompletableArrayCreationElement(psiElement);
                    if(arrayCreationExpression == null || !(arrayCreationExpression.getContext() instanceof PhpReturn phpReturn)) {
                        return new PsiReference[0];
                    }

                    Method method = PsiTreeUtil.getParentOfType(phpReturn, Method.class);
                    if(method == null) {
                        return new PsiReference[0];
                    }

                    if (!PhpElementsUtil.isMethodInstanceOf(method, "\\Symfony\\Component\\EventDispatcher\\EventSubscriberInterface", "getSubscribedEvents")) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[] { new EventDispatcherEventReference((StringLiteralExpression) psiElement) };
                }

                @Override
                public boolean acceptsTarget(@NotNull PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );

        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {
                @Override
                public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
                    if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement instanceof StringLiteralExpression stringLiteralExpression)) {
                        return PsiReference.EMPTY_ARRAY;
                    }

                    if (!PhpArrayServiceUtil.isInsidePhpArrayServiceConfig(stringLiteralExpression)) {
                        return PsiReference.EMPTY_ARRAY;
                    }

                    Collection<PsiReference> references = new ArrayList<>();
                    PhpArrayServiceUtil.ServiceConfigPath keyPath = PhpArrayServiceUtil.getKeyPath(stringLiteralExpression);

                    if (PhpArrayServiceUtil.isServiceLevelAliasValue(stringLiteralExpression)) {
                        attachServiceReference(references, stringLiteralExpression, stringLiteralExpression.getContents(), 1, 0);
                    }

                    if (keyPath == null) {
                        return references.toArray(PsiReference.EMPTY_ARRAY);
                    }

                    if (keyPath.isDecoratesOrParent()) {
                        // 'decorates' / 'parent' => service id
                        references.add(new ServiceReference(stringLiteralExpression, true));
                    } else if (keyPath.isClass()) {
                        // 'class' => FQCN string
                        references.add(new PhpClassReference(stringLiteralExpression, true));
                    } else if (keyPath.isArgument()) {
                        // 'arguments' and nested call arguments
                        attachArgumentReferences(references, stringLiteralExpression);
                    } else if (keyPath.isTag()) {
                        // 'tags' => ['tag'] or ['name' => 'tag']
                        references.add(new TagReference(stringLiteralExpression));
                    } else if (keyPath.isFactoryService()) {
                        // 'factory' => ['@service', 'method']
                        attachServiceReference(references, stringLiteralExpression, stringLiteralExpression.getContents(), 1, 0);
                    } else if (keyPath.isFactoryMethod()) {
                        // 'factory' => ['@service', 'method']
                        String factoryClass = getFactoryClass(stringLiteralExpression);
                        if (StringUtils.isNotBlank(factoryClass)) {
                            references.add(new ClassPublicMethodReference(stringLiteralExpression, factoryClass));
                        }
                    } else if (keyPath.isCallsMethod()) {
                        // 'calls' => [['method', [...]]]
                        String serviceClass = PhpArrayServiceUtil.getCurrentServiceClass(stringLiteralExpression);
                        if (StringUtils.isNotBlank(serviceClass)) {
                            references.add(new ClassPublicMethodReference(stringLiteralExpression, serviceClass));
                        }
                    }

                    return references.toArray(PsiReference.EMPTY_ARRAY);
                }
            }
        );
    }

    private static boolean phpStringLiteralExpressionClassReference(String signature, int index, PsiElement psiElement) {

        if (!(psiElement.getContext() instanceof ParameterList parameterList)) {
            return false;
        }

        if (!(parameterList.getContext() instanceof NewExpression newExpression)) {
            return false;
        }

        if(PsiElementUtils.getParameterIndexValue(psiElement) != index) {
            return false;
        }

        ClassReference classReference = newExpression.getClassReference();
        if(classReference == null) {
            return false;
        }

        return classReference.getSignature().equals("#C" + signature);
    }

    private static void attachArgumentReferences(@NotNull Collection<PsiReference> references, @NotNull StringLiteralExpression stringLiteralExpression) {
        String contents = stringLiteralExpression.getContents();
        if (StringUtils.isBlank(contents)) {
            return;
        }

        if (contents.startsWith("@") && contents.length() > 1) {
            attachServiceReference(references, stringLiteralExpression, contents, 1, 0);
            return;
        }

        if (isClassString(contents)) {
            references.add(new PhpClassReference(stringLiteralExpression, true));
        }
    }

    private static void attachServiceReference(@NotNull Collection<PsiReference> references, @NotNull StringLiteralExpression stringLiteralExpression, @NotNull String contents, int trimLeft, int trimRight) {
        if (contents.length() <= trimLeft + trimRight) {
            return;
        }

        references.add(new TrimmedServiceReference(stringLiteralExpression, createValueRange(contents, trimLeft, trimRight), contents.substring(trimLeft, contents.length() - trimRight)));
    }

    @NotNull
    private static TextRange createValueRange(@NotNull String contents, int trimLeft, int trimRight) {
        return TextRange.from(1 + trimLeft, contents.length() - trimLeft - trimRight);
    }

    private static boolean isClassString(@NotNull String contents) {
        return contents.contains("\\") && !contents.startsWith("@") && !contents.startsWith("%");
    }

    @Nullable
    private static String getFactoryClass(@NotNull StringLiteralExpression stringLiteralExpression) {
        ArrayCreationExpression factoryArray = PsiTreeUtil.getParentOfType(stringLiteralExpression, ArrayCreationExpression.class);
        if (factoryArray == null) {
            return null;
        }

        PsiElement[] arrayValues = PhpElementsUtil.getArrayValues(factoryArray);
        PsiElement firstValue = arrayValues.length > 0 ? arrayValues[0] : null;

        String factoryValue = PhpArrayServiceUtil.getPsiValue(firstValue);
        if (StringUtils.isBlank(factoryValue)) {
            return null;
        }

        if (factoryValue.startsWith("@")) {
            return ContainerCollectionResolver.resolveService(stringLiteralExpression.getProject(), factoryValue.substring(1));
        }

        return StringUtils.stripStart(factoryValue, "\\");
    }

    private static class TrimmedServiceReference extends AbstractServiceReference {
        private TrimmedServiceReference(@NotNull StringLiteralExpression element, @NotNull TextRange textRange, @NotNull String serviceId) {
            super(element, textRange);
            this.serviceId = serviceId;
        }
    }

}
