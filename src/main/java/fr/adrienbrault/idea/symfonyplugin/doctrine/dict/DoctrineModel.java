package fr.adrienbrault.idea.symfonyplugin.doctrine.dict;

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

    public DoctrineModel(@NotNull String clazz, @Nullable String repositoryClass) {
        this.clazz = clazz;
        this.repositoryClass = repositoryClass;
    }

    @NotNull
    public String getClassName() {
        return this.clazz;
    }

    @Nullable
    public String getRepositoryClass() {
        return repositoryClass;
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hash = new HashCodeBuilder()
            .append(this.clazz);

        String repositoryClass = this.repositoryClass;

        // null != ""
        if(repositoryClass == null) {
            repositoryClass = "null";
        }

        hash.append(repositoryClass);

        return hash.toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DoctrineModel &&
            Objects.equals(((DoctrineModel) obj).clazz, this.clazz) &&
            Objects.equals(((DoctrineModel) obj).repositoryClass, this.repositoryClass)
        ;
    }
}
