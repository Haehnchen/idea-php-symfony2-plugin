package fr.adrienbrault.idea.symfony2plugin.stubs.dict;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DispatcherEvent implements Serializable {

    @Nullable
    private String fqn;

    @Nullable
    private String instance;

    public DispatcherEvent() {
    }

    public DispatcherEvent(@NotNull String fqn, @Nullable String instance) {
        this.fqn = fqn;
        this.instance = instance;
    }

    @Nullable
    public String getFqn() {
        return fqn;
    }

    @Nullable
    public String getInstance() {
        return instance;
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.fqn)
            .append(this.instance)
            .toHashCode()
        ;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DispatcherEvent &&
            Objects.equals(((DispatcherEvent) obj).fqn, this.fqn) &&
            Objects.equals(((DispatcherEvent) obj).instance, this.instance)
        ;
    }
}
