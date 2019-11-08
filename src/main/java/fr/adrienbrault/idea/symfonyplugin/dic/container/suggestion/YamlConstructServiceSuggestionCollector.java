package fr.adrienbrault.idea.symfonyplugin.dic.container.suggestion;

import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfonyplugin.dic.ContainerService;
import fr.adrienbrault.idea.symfonyplugin.dic.container.suggestion.utils.ServiceSuggestionUtil;
import fr.adrienbrault.idea.symfonyplugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfonyplugin.stubs.ContainerCollectionResolver;
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
