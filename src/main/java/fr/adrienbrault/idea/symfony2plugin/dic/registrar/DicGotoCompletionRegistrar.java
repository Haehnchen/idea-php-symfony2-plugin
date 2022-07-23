package fr.adrienbrault.idea.symfony2plugin.dic.registrar;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.config.component.ParameterLookupElement;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.completion.TagNameCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DicGotoCompletionRegistrar implements GotoCompletionRegistrar {
    @Override
    public void register(@NotNull GotoCompletionRegistrarParameter registrar) {
        // getParameter('FOO')
        registrar.register(
            PlatformPatterns.psiElement().withParent(PhpElementsUtil.getMethodWithFirstStringPattern()), psiElement -> {

                PsiElement context = psiElement.getContext();
                if (!(context instanceof StringLiteralExpression)) {
                    return null;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterRecursiveMatcher(context, 0)
                    .withSignature("\\Symfony\\Component\\DependencyInjection\\ContainerInterface", "hasParameter")
                    .withSignature("\\Symfony\\Component\\DependencyInjection\\ContainerInterface", "getParameter")
                    .withSignature("\\Symfony\\Component\\DependencyInjection\\Loader\\Configurator\\ParametersConfigurator", "set")
                    .withSignature("\\Symfony\\Component\\DependencyInjection\\ParameterBag\\ParameterBagInterface", "get")
                    .withSignature("\\Symfony\\Component\\DependencyInjection\\ParameterBag\\ParameterBagInterface", "set")
                    .withSignature("\\Symfony\\Component\\DependencyInjection\\ParameterBag\\ParameterBagInterface", "has")
                    .match();

                if(methodMatchParameter == null) {
                    return null;
                }

                return new ParameterContributor((StringLiteralExpression) context);
            }
        );

        // param('<caret>')
        registrar.register(
            PlatformPatterns.psiElement().withParent(PhpElementsUtil.getFunctionWithFirstStringPattern("param")), psiElement -> {
                PsiElement context = psiElement.getContext();
                if (!(context instanceof StringLiteralExpression)) {
                    return null;
                }

                return new ParameterContributor((StringLiteralExpression) context);
            }
        );

        // #[Autowire('<caret>')]
        // #[Autowire(value: '<caret>')]
        registrar.register(
            PlatformPatterns.or(
                PhpElementsUtil.getFirstAttributeStringPattern(ServiceContainerUtil.AUTOWIRE_ATTRIBUTE_CLASS),
                PhpElementsUtil.getAttributeNamedArgumentStringPattern(ServiceContainerUtil.AUTOWIRE_ATTRIBUTE_CLASS, "value")
            ), psiElement -> {
                PsiElement context = psiElement.getContext();
                if (!(context instanceof StringLiteralExpression)) {
                    return null;
                }

                PhpAttribute phpAttribute = PsiTreeUtil.getParentOfType(context, PhpAttribute.class);
                if (phpAttribute != null) {
                    return new ParameterContributor((StringLiteralExpression) context);
                }

                return null;
            }
        );

        // #[TaggedIterator('app.handler')] iterable $handlers
        // #[TaggedLocator('app.handler')] ContainerInterface $handlers
        registrar.register(
            PlatformPatterns.or(
                PhpElementsUtil.getFirstAttributeStringPattern(ServiceContainerUtil.TAGGED_ITERATOR_ATTRIBUTE_CLASS),
                PhpElementsUtil.getAttributeNamedArgumentStringPattern(ServiceContainerUtil.TAGGED_ITERATOR_ATTRIBUTE_CLASS, "tag"),
                PhpElementsUtil.getFirstAttributeStringPattern(ServiceContainerUtil.TAGGED_LOCATOR_ATTRIBUTE_CLASS),
                PhpElementsUtil.getAttributeNamedArgumentStringPattern(ServiceContainerUtil.TAGGED_LOCATOR_ATTRIBUTE_CLASS, "tag")
            ), psiElement -> {
                PsiElement context = psiElement.getContext();
                if (!(context instanceof StringLiteralExpression)) {
                    return null;
                }

                PhpAttribute phpAttribute = PsiTreeUtil.getParentOfType(context, PhpAttribute.class);
                if (phpAttribute != null) {
                    return new TaggedIteratorContributor((StringLiteralExpression) context);
                }

                return null;
            }
        );

        // #[Autowire(service: 'some_service')]
        registrar.register(
            PhpElementsUtil.getAttributeNamedArgumentStringPattern(ServiceContainerUtil.AUTOWIRE_ATTRIBUTE_CLASS, "service"),
            psiElement -> {
                PsiElement context = psiElement.getContext();
                if (!(context instanceof StringLiteralExpression)) {
                    return null;
                }

                PhpAttribute phpAttribute = PsiTreeUtil.getParentOfType(context, PhpAttribute.class);
                if (phpAttribute != null) {
                    return new ServiceContributor((StringLiteralExpression) context);
                }

                return null;
            }
        );
    }

    private static class ParameterContributor extends GotoCompletionProvider {

        public ParameterContributor(StringLiteralExpression element) {
            super(element);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            Collection<LookupElement> results = new ArrayList<>();

            for (Map.Entry<String, ContainerParameter> entry: ContainerCollectionResolver.getParameters(getElement().getProject()).entrySet()) {
                results.add(new ParameterLookupElement(entry.getValue()));
            }

            return results;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String contents = GotoCompletionUtil.getStringLiteralValue(element);
            if (contents == null) {
                return Collections.emptyList();
            }

            return ServiceUtil.getParameterDefinition(element.getProject(), contents);
        }
    }

    private static class TaggedIteratorContributor extends GotoCompletionProvider {
        public TaggedIteratorContributor(StringLiteralExpression element) {
            super(element);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            return TagNameCompletionProvider.getTagLookupElements(getElement().getProject());
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String contents = GotoCompletionUtil.getStringLiteralValue(element);
            if(contents == null) {
                return Collections.emptyList();
            }

            return new ArrayList<>(ServiceUtil.getTaggedClasses(getElement().getProject(), contents));
        }
    }

    private static class ServiceContributor extends GotoCompletionProvider {
        public ServiceContributor(@NotNull StringLiteralExpression element) {
            super(element);
        }

        @Override
        public @NotNull Collection<LookupElement> getLookupElements() {
            return ServiceCompletionProvider.getLookupElements(this.getElement(), ContainerCollectionResolver.getServices(getProject()).values()).getLookupElements();
        }

        @Override
        public @NotNull Collection<PsiElement> getPsiTargets(PsiElement element) {
            String contents = GotoCompletionUtil.getStringLiteralValue(element);
            if (contents == null) {
                return Collections.emptyList();
            }

            PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(element.getProject(), contents);
            if (phpClass == null) {
                return Collections.emptyList();
            }

            return List.of(phpClass);
        }
    }
}
