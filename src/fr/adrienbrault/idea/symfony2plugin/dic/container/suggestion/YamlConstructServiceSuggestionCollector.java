package fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion;

import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.dict.ServiceTypeHint;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlConstructServiceSuggestionCollector implements ServiceSuggestionCollector {

    @NotNull
    public Collection<String> collect(@NotNull PsiElement psiElement, @NotNull Collection<ContainerService> serviceMap) {
        ServiceTypeHint methodTypeHint = ServiceContainerUtil.getYamlConstructorTypeHint(
            psiElement, new ContainerCollectionResolver.LazyServiceCollector(psiElement.getProject())
        );

        if(methodTypeHint == null) {
            return Collections.emptyList();
        }

        Collection<ContainerService> suggestions = ServiceUtil.getServiceSuggestionsForTypeHint(
            methodTypeHint.getMethod(),
            methodTypeHint.getIndex(),
            serviceMap
        );

        if(suggestions.size() == 0) {
            return Collections.emptyList();
        }

        return ContainerUtil.map(suggestions, ContainerService::getName);
    }
}
