package fr.adrienbrault.idea.symfonyplugin.extension;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ServiceCollector {

    /**
     * Warning expect high traffic, collector needs to be highly optimized
     */
    void collectServices(@NotNull ServiceCollectorParameter.Service parameter);

    /**
     * Warning expect high traffic, collector needs to be highly optimized
     */
    void collectIds(@NotNull ServiceCollectorParameter.Id parameter);
}
