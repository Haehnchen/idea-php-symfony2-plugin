package fr.adrienbrault.idea.symfony2plugin.doctrine.dict;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineModel implements DoctrineModelInterface {

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

    public DoctrineModelInterface setRepositoryClass(@Nullable String repositoryClass) {
        this.repositoryClass = repositoryClass;
        return this;
    }
}
