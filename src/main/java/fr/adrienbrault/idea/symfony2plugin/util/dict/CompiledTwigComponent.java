package fr.adrienbrault.idea.symfony2plugin.util.dict;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Symfony UX TwigComponent metadata extracted from compiled container XML.
 */
public record CompiledTwigComponent(
    @NotNull String name,
    @Nullable String phpClass,
    @Nullable String template,
    @Nullable String templateFromMethod
) {
}
