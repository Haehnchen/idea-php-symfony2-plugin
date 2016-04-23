package fr.adrienbrault.idea.symfony2plugin.dic.container;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * We dont want to serialize default values so eg use Boolean and nullable values for properties
 */
public class SerializableService implements ServiceInterface, Serializable {

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
}
