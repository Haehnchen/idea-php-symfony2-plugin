package fr.adrienbrault.idea.symfony2plugin.profiler.collector;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface DefaultDataCollectorInterface {
    @Nullable
    String getController();

    @Nullable
    String getRoute();

    @Nullable
    String getTemplate();

    @NotNull
    Collection<String> getFormTypes();
}
