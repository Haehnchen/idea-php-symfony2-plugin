package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Primitive metadata for a Symfony form field found in a form type.
 *
 * @param name visible form field name
 * @param fieldTypeFqn explicit field form type FQN, or {@code null} when no type is declared
 * @param ownerFormTypeFqn FQN of the form type contributing this field
 * @param propertyName PHP property declaring an attribute field, or {@code null} for builder fields
 * @param options statically known options for nested form types
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record TwigFormField(
    @NotNull String name,
    @Nullable String fieldTypeFqn,
    @NotNull String ownerFormTypeFqn,
    @Nullable String propertyName,
    @NotNull Map<String, String> options
) {
    public TwigFormField {
        options = Map.copyOf(options);
    }
}
