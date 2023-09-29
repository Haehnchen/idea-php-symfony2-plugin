package fr.adrienbrault.idea.symfony2plugin.stubs.dict;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;
import java.util.TreeMap;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FileResource implements Serializable {
    private final String resource;
    private final FileResourceContextTypeEnum contextType;
    private final TreeMap<String, String> contextValues;

    public FileResource(@Nullable String resource, @Nullable FileResourceContextTypeEnum contextType, @Nullable TreeMap<String, String> contextValues) {
        this.resource = resource;
        this.contextType = contextType;
        this.contextValues = contextValues;
    }

    @Nullable
    public String getResource() {
        return resource;
    }

    @Nullable
    public FileResourceContextTypeEnum getContextType() {
        return contextType;
    }

    @Nullable
    public TreeMap<String, String> getContextValues() {
        return contextValues;
    }

    @Nullable
    public String getPrefix() {
        return contextValues == null ? null : contextValues.get("prefix");
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.resource)
            .append(this.contextType != null ? this.contextType.toString() : "")
            .append(this.contextValues != null ? this.contextValues.hashCode() : "")
            .toHashCode()
        ;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FileResource &&
            Objects.equals(((FileResource) obj).resource, this.resource) &&
            Objects.equals(((FileResource) obj).contextType, this.contextType) &&
            Objects.equals(((FileResource) obj).contextValues, this.contextValues)
        ;
    }
}
