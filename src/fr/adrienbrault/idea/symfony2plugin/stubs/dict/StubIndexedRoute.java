package fr.adrienbrault.idea.symfony2plugin.stubs.dict;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public class StubIndexedRoute {

    @NotNull
    private final String name;

    @Nullable
    private String controller = null;

    @Nullable
    private String path = null;

    @NotNull
    private Collection<String> methods = new HashSet<String>();

    public StubIndexedRoute(@NotNull String name) {
        this.name = name;
    }

    @Nullable
    public String getController() {
        return controller;
    }

    public void setController(@Nullable String controller) {
        this.controller = controller;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @Nullable
    public String getPath() {
        return path;
    }

    public void setPath(@Nullable String path) {
        this.path = path;
    }

    @NotNull
    public Collection<String> getMethods() {
        return methods;
    }

    public void setMethods(@NotNull Collection<String> methods) {
        this.methods = methods;
    }

    public void addMethod(@NotNull String[] content) {
        this.methods.addAll(Arrays.asList(content));
    }
}