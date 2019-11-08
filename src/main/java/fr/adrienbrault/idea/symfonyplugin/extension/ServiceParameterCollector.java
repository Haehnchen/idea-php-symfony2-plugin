package fr.adrienbrault.idea.symfony2plugin.extension;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ServiceParameterCollector {
    /**
     * Warning expect high traffic, collector needs to be highly optimized
     */
    void collectIds(@NotNull ServiceParameterCollectorParameter.Id parameter);
}
