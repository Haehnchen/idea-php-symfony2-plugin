package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Primitive metadata for a Symfony form field found in a form type.
 *
 * @param name field name passed to the form builder
 * @param fieldTypeFqn explicit field form type FQN, or {@code null} when the builder call has no type parameter
 * @param ownerFormTypeFqn FQN of the form type whose {@code buildForm()} contributed this field
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record TwigFormField(
    @NotNull String name,
    @Nullable String fieldTypeFqn,
    @NotNull String ownerFormTypeFqn
) {
}
