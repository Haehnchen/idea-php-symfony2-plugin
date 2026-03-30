package fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion;

import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.utils.ServiceSuggestionUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Suggests services based on the constructor argument type-hint for PHP array-style Symfony service configuration.
 *
 * <pre>
 * MyService::class => [
 *     'arguments' => [
 *         service('<caret>'),
 *         ref('<caret>'),
 *         '@<caret>',
 *         '$logger' => service('<caret>'),
 *     ],
 * ]
 * </pre>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpArrayArgumentServiceSuggestionCollector implements ServiceSuggestionCollector {

    @NotNull
    @Override
    public Collection<String> collect(@NotNull PsiElement psiElement, @NotNull Collection<ContainerService> serviceMap) {
        return ServiceSuggestionUtil.createSuggestions(
            ServiceContainerUtil.getPhpArrayConstructorTypeHint(
                psiElement,
                new ContainerCollectionResolver.LazyServiceCollector(psiElement.getProject())
            ),
            serviceMap
        );
    }
}
