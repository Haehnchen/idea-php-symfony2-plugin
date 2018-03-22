package fr.adrienbrault.idea.symfony2plugin.stubs.dict;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FileResource implements Serializable {

    private final String resource;
    private String prefix = null;

    public FileResource(@Nullable String resource) {
        this.resource = resource;
    }

    @Nullable
    public String getResource() {
        return resource;
    }

    public FileResource setPrefix(@Nullable String prefix) {
        this.prefix = prefix;
        return this;
    }

    public String getPrefix() {
        return prefix;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.resource)
            .append(this.prefix)
            .toHashCode()
        ;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FileResource &&
            Objects.equals(((FileResource) obj).resource, this.resource) &&
            Objects.equals(((FileResource) obj).prefix, this.prefix)
        ;
    }
}
