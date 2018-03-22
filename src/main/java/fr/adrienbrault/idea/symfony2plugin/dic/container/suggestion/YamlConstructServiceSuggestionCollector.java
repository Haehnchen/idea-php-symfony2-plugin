package fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion;

import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.utils.ServiceSuggestionUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlConstructServiceSuggestionCollector implements ServiceSuggestionCollector {

    @NotNull
    public Collection<String> collect(@NotNull PsiElement psiElement, @NotNull Collection<ContainerService> serviceMap) {
        return ServiceSuggestionUtil.createSuggestions(ServiceContainerUtil.getYamlConstructorTypeHint(
            psiElement, new ContainerCollectionResolver.LazyServiceCollector(psiElement.getProject())
        ), serviceMap);
    }
}
