package fr.adrienbrault.idea.symfony2plugin.util.dict;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record TwigComponentNamespace(@NotNull String namespace, @NotNull String templateDirectory, @Nullable String namePrefix) {
}
