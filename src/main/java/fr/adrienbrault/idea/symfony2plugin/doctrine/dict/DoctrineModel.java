package fr.adrienbrault.idea.symfony2plugin.doctrine.dict;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.HashCodeBuilder;
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

    @Nullable
    @SerializedName("table_name")
    private String tableName;

    public DoctrineModel(@NotNull String clazz) {
        this.clazz = clazz;
    }

    public DoctrineModel(@NotNull String clazz, @Nullable String repositoryClass) {
        this.clazz = clazz;
        this.repositoryClass = repositoryClass;
    }

    public DoctrineModel(@NotNull String clazz, @Nullable String repositoryClass, @Nullable String tableName) {
        this.clazz = clazz;
        this.repositoryClass = repositoryClass;
        this.tableName = tableName;
    }

    @NotNull
    public String getClassName() {
        return this.clazz;
    }

    @Nullable
    public String getRepositoryClass() {
        return repositoryClass;
    }

    @Nullable
    @Override
    public String getTableName() {
        return tableName;
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

        String tableName = this.tableName;
        if (tableName == null) {
            tableName = "null";
        }
        hash.append(tableName);

        return hash.toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DoctrineModel &&
            Objects.equals(((DoctrineModel) obj).clazz, this.clazz) &&
            Objects.equals(((DoctrineModel) obj).repositoryClass, this.repositoryClass) &&
            Objects.equals(((DoctrineModel) obj).tableName, this.tableName)
        ;
    }
}
