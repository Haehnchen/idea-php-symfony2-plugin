package fr.adrienbrault.idea.symfony2plugin.doctrine.dict;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Internal data class holding parsed Doctrine class metadata (className, repositoryClass, tableName).
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record DoctrineClassMetadata(
    @NotNull String className,
    @Nullable String repositoryClass,
    @Nullable String tableName
) {
}
