package fr.adrienbrault.idea.symfony2plugin.dic;

import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceInterface;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class ServiceMap {
    @NotNull
    final private Collection<ServiceInterface> services;

    private Collection<String> ids;

    ServiceMap() {
        this.services = Collections.emptyList();
    }

    ServiceMap(@NotNull Collection<ServiceInterface> services) {
        this.services = Collections.unmodifiableCollection(services);
    }

    public Collection<String> getIds() {
        // cache value, instance cached until invalidated
        if(ids != null) {
            return ids;
        }

        Collection<String> map = new HashSet<>();

        services.forEach(service ->
            map.add(service.getId())
        );

        return this.ids = Collections.unmodifiableCollection(map);
    }

    @NotNull
    public Collection<ServiceInterface> getServices() {
        return services;
    }
}
