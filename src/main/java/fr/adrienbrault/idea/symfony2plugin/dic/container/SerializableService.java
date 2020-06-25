package fr.adrienbrault.idea.symfony2plugin.dic.container;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * We dont want to serialize default values so eg use Boolean and nullable values for properties
 */
public class SerializableService implements ServiceSerializable {

    @NotNull
    private final String id;

    @SerializedName("class")
    @Nullable
    private String className;

    @SerializedName("public")
    @Nullable
    private Boolean isPublic;

    @SerializedName("lazy")
    @Nullable
    private Boolean isLazy;

    @SerializedName("abstract")
    @Nullable
    private Boolean isAbstract;

    @SerializedName("autowire")
    @Nullable
    private Boolean isAutowire;

    @SerializedName("deprecated")
    @Nullable
    private Boolean isDeprecated;

    @Nullable
    private String alias;

    @Nullable
    private String decorates;

    @Nullable
    @SerializedName("decoration_inner_name")
    private String decorationInnerName;

    @Nullable
    private String parent;

    @Nullable
    @SerializedName("resource")
    private String resource;

    @Nullable
    @SerializedName("exclude")
    private String exclude;

    public SerializableService(@NotNull String id) {
        this.id = id;
    }

    @Override
    @NotNull
    public String getId() {
        return id;
    }

    @Override
    @Nullable
    public String getClassName() {
        return className;
    }

    public SerializableService setClassName(@Nullable String className) {
        this.className = className;
        return this;
    }

    public SerializableService setIsPublic(@Nullable Boolean isPublic) {
        this.isPublic = isPublic;
        return this;
    }

    @Override
    public boolean isLazy() {
        return isLazy != null ? isLazy : false;
    }

    public SerializableService setIsLazy(@Nullable Boolean isLazy) {
        this.isLazy = isLazy;
        return this;
    }

    @Override
    public boolean isAbstract() {
        return isAbstract != null ? isAbstract : false;
    }

    public SerializableService setIsAbstract(@Nullable Boolean isAbstract) {
        this.isAbstract = isAbstract;
        return this;
    }

    @Override
    public boolean isAutowire() {
        return isAutowire != null ? isAutowire : false;
    }

    public SerializableService setIsAutowire(@Nullable Boolean isAutowire) {
        this.isAutowire = isAutowire;
        return this;
    }

    @Override
    public boolean isDeprecated() {
        return isDeprecated != null ? isDeprecated : false;
    }

    public SerializableService setIsDeprecated(@Nullable Boolean isDeprecated) {
        this.isDeprecated = isDeprecated;
        return this;
    }

    @Override
    public boolean isPublic() {
        return isPublic != null ? isPublic : true;
    }

    @Nullable
    @Override
    public String getAlias() {
        return alias;
    }

    public SerializableService setAlias(@Nullable String alias) {
        this.alias = alias;
        return this;
    }

    @Override
    @Nullable
    public String getParent() {
        return parent;
    }

    public SerializableService setParent(@Nullable String parent) {
        this.parent = parent;
        return this;
    }

    @Override
    @Nullable
    public String getDecorates() {
        return decorates;
    }

    public SerializableService setDecorates(@Nullable String decorates) {
        this.decorates = decorates;
        return this;
    }

    @Override
    @Nullable
    public String getDecorationInnerName() {
        return decorationInnerName;
    }

    public SerializableService setDecorationInnerName(@Nullable String decorationInnerName) {
        this.decorationInnerName = decorationInnerName;
        return this;
    }

    @Nullable
    @Override
    public String getResource() {
        return this.resource;
    }

    @Nullable
    @Override
    public String getExclude() {
        return this.exclude;
    }

    public SerializableService setResource(@Nullable String resource) {
        this.resource = resource;
        return this;
    }

    public SerializableService setExclude(@Nullable String exclude) {
        this.exclude = exclude;
        return this;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.id)
            .append(this.className)
            .append(this.isPublic)
            .append(this.isDeprecated)
            .append(this.isLazy)
            .append(this.isAbstract)
            .append(this.isAutowire)
            .append(this.isDeprecated)
            .append(this.alias)
            .append(this.decorates)
            .append(this.decorationInnerName)
            .append(this.parent)
            .append(this.resource)
            .append(this.exclude)
            .toHashCode()
        ;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SerializableService &&
            Objects.equals(((SerializableService) obj).id, this.id) &&
            Objects.equals(((SerializableService) obj).className, this.className) &&
            Objects.equals(((SerializableService) obj).isPublic, this.isPublic) &&
            Objects.equals(((SerializableService) obj).isDeprecated, this.isDeprecated) &&
            Objects.equals(((SerializableService) obj).isLazy, this.isLazy) &&
            Objects.equals(((SerializableService) obj).isAbstract, this.isAbstract) &&
            Objects.equals(((SerializableService) obj).isAutowire, this.isAutowire) &&
            Objects.equals(((SerializableService) obj).isDeprecated, this.isDeprecated) &&
            Objects.equals(((SerializableService) obj).alias, this.alias) &&
            Objects.equals(((SerializableService) obj).decorates, this.decorates) &&
            Objects.equals(((SerializableService) obj).decorationInnerName, this.decorationInnerName) &&
            Objects.equals(((SerializableService) obj).parent, this.parent) &&
            Objects.equals(((SerializableService) obj).resource, this.resource) &&
            Objects.equals(((SerializableService) obj).exclude, this.exclude)
        ;
    }
}
