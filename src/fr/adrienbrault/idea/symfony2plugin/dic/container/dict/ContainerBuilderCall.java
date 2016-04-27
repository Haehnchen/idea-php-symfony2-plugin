package fr.adrienbrault.idea.symfony2plugin.dic.container.dict;

import com.intellij.util.containers.HashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ContainerBuilderCall implements Serializable {

    @Nullable
    private String scope;

    @Nullable
    private String name;

    private Collection<String> parameter = new HashSet<>();

    public ContainerBuilderCall() {
    }

    public ContainerBuilderCall(@NotNull String scope) {
        this.scope = scope;
    }

    @Nullable
    public String getScope() {
        return scope;
    }

    public void setScope(@Nullable String scope) {
        this.scope = scope;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    @Nullable
    public Collection<String> getParameter() {
        return parameter;
    }

    public void addParameter(@NotNull String parameter) {
        this.parameter.add(parameter);
    }
}
