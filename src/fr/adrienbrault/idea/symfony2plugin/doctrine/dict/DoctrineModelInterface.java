package fr.adrienbrault.idea.symfony2plugin.doctrine.dict;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface DoctrineModelInterface extends Serializable {

    @NotNull
    String getClassName();

    @Nullable
    String getRepositoryClass();
}
