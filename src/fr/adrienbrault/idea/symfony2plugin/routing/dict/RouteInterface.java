package fr.adrienbrault.idea.symfony2plugin.routing.dict;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface RouteInterface {

    @NotNull
    String getName();

    @Nullable
    String getController();

    @Nullable
    String getPath();

    @NotNull
    Collection<String> getMethods();
}
