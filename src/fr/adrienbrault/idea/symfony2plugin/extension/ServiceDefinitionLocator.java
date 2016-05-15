package fr.adrienbrault.idea.symfony2plugin.extension;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ServiceDefinitionLocator {

    /**
     * Find service declaration by id
     */
    void locate(@NotNull String serviceId, @NotNull ServiceDefinitionLocatorParameter parameter);
}
