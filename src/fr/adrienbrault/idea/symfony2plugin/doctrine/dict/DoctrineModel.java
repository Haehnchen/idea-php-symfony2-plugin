package fr.adrienbrault.idea.symfony2plugin.doctrine.dict;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineModel implements DoctrineModelSerializable {

    @NotNull
    private final String clazz;

    @Nullable
    @SerializedName("repository_class")
    private String repositoryClass;

    public DoctrineModel(@NotNull String clazz) {
        this.clazz = clazz;
    }

    @NotNull
    public String getClassName() {
        return this.clazz;
    }

    @Nullable
    public String getRepositoryClass() {
        return repositoryClass;
    }

    public DoctrineModel setRepositoryClass(@Nullable String repositoryClass) {
        this.repositoryClass = repositoryClass;
        return this;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.clazz)
            .append(this.repositoryClass)
            .toHashCode()
        ;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DoctrineModel &&
            Objects.equals(((DoctrineModel) obj).clazz, this.clazz) &&
            Objects.equals(((DoctrineModel) obj).repositoryClass, this.repositoryClass)
        ;
    }
}
