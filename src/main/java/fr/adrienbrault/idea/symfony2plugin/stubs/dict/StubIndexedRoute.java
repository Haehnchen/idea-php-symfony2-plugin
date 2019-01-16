package fr.adrienbrault.idea.symfony2plugin.stubs.dict;

import fr.adrienbrault.idea.symfony2plugin.routing.dict.RouteInterface;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class StubIndexedRoute implements RouteInterface, Serializable {

    @NotNull
    private final String name;

    @Nullable
    private String controller = null;

    @Nullable
    private String path = null;

    @NotNull
    private Collection<String> methods = new HashSet<>();

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

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.name)
            .append(this.controller)
            .append(this.path)
            .append(new HashSet<>(this.methods))
            .toHashCode()
        ;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RouteInterface &&
             Objects.equals(((RouteInterface) obj).getName(), this.name) &&
             Objects.equals(((RouteInterface) obj).getController(), this.controller) &&
             Objects.equals(((RouteInterface) obj).getPath(), this.path) &&
             Objects.equals(new HashSet<>(((RouteInterface) obj).getMethods()), new HashSet<>(this.methods))
        ;
    }
}