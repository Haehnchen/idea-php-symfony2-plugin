package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Primitive metadata for a Symfony form field found in a form type.
 *
 * @param name visible form field name
 * @param fieldTypeFqn explicit field form type FQN, or {@code null} when no type is declared
 * @param ownerFormTypeFqn FQN of the form type contributing this field
 * @param propertyName PHP property declaring an attribute field, or {@code null} for builder fields
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record TwigFormField(
    @NotNull String name,
    @Nullable String fieldTypeFqn,
    @NotNull String ownerFormTypeFqn,
    @Nullable String propertyName
) {
}
