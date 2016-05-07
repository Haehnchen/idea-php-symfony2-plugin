package fr.adrienbrault.idea.symfony2plugin.routing.dict;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class JsonRoute implements RouteInterface {

    @NotNull
    private final String name;

    @Nullable
    private String controller;

    @Nullable
    private String path;

    @Nullable
    private Collection<String> methods;

    public JsonRoute(@NotNull String name) {
        this.name = name;
    }

    @Override
    @NotNull
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    public String getController() {
        return this.controller;
    }

    @Nullable
    @Override
    public String getPath() {
        return this.path;
    }

    @NotNull
    @Override
    public Collection<String> getMethods() {
        return this.methods == null ? Collections.emptyList() : this.methods;
    }

    public JsonRoute setPath(@Nullable String path) {
        this.path = path;

        return this;
    }

    public JsonRoute setController(@Nullable String controller) {
        this.controller = controller;

        return this;
    }

    public JsonRoute setMethods(@Nullable Collection<String> methods) {
        this.methods = methods;

        return this;
    }
}
