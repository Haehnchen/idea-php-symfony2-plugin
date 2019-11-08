package fr.adrienbrault.idea.symfonyplugin.extension;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface RoutingLoader {
    void invoke(@NotNull RoutingLoaderParameter parameter);
}
