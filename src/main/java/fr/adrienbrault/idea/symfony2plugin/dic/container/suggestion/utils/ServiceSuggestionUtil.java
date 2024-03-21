package fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.utils;

import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.dict.ServiceTypeHint;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceSuggestionUtil {

    @NotNull
    private static Collection<String> createServiceCollection(@NotNull ServiceTypeHint serviceTypeHint, @NotNull Collection<ContainerService> serviceMap) {
        Collection<ContainerService> suggestions = ServiceUtil.getServiceSuggestionsForTypeHint(
            serviceTypeHint.getMethod(),
            serviceTypeHint.getIndex(),
            serviceMap
        );

        if(suggestions.isEmpty()) {
            return Collections.emptyList();
        }

        return ContainerUtil.map(suggestions, ContainerService::getName);
    }

    @NotNull
    public static Collection<String> createSuggestions(@Nullable ServiceTypeHint serviceTypeHint, @NotNull Collection<ContainerService> serviceMap) {
        if(serviceTypeHint == null) {
            return Collections.emptyList();
        }

        return ServiceSuggestionUtil.createServiceCollection(serviceTypeHint, serviceMap);
    }
}
