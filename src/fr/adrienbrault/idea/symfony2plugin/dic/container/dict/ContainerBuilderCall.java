package fr.adrienbrault.idea.symfony2plugin.dic.container.dict;

import com.intellij.util.containers.HashSet;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

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


    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.scope)
            .append(this.name)
            .append(new java.util.HashSet<>(this.parameter))
            .toHashCode()
        ;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ContainerBuilderCall &&
            Objects.equals(((ContainerBuilderCall) obj).scope, this.scope) &&
            Objects.equals(((ContainerBuilderCall) obj).name, this.scope) &&
            Objects.equals(new java.util.HashSet<>(((ContainerBuilderCall) obj).parameter), new java.util.HashSet<>(this.parameter))
        ;
    }

}
