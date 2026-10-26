package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Metadata for a Symfony form field exposed in Twig, for example {@code form.title}.
 *
 * @param fieldTypeFqn explicit normalized field form type FQN, or {@code null} when no type is declared
 * @param ownerFormTypeFqn normalized FQN of the form type contributing this field
 * @param propertyName PHP property declaring an attribute field, or {@code null} for builder fields
 * @param options statically known options for nested form types
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record FormFieldDataHolder(
    @Nullable String fieldTypeFqn,
    @NotNull String ownerFormTypeFqn,
    @Nullable String propertyName,
    @NotNull Map<String, String> options
) {
    public FormFieldDataHolder {
        options = Map.copyOf(options);
        if (fieldTypeFqn != null && !fieldTypeFqn.startsWith("\\")) {
            throw new IllegalArgumentException("fieldTypeFqn must be normalized with a leading backslash");
        }

        if (!ownerFormTypeFqn.startsWith("\\")) {
            throw new IllegalArgumentException("ownerFormTypeFqn must be normalized with a leading backslash");
        }
    }
}
