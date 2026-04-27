package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Metadata for a Symfony form field exposed in Twig, for example {@code form.title}.
 *
 * @param fieldTypeFqn explicit normalized field form type FQN, or {@code null} when the builder call has no type parameter
 * @param ownerFormTypeFqn normalized FQN of the form type whose {@code buildForm()} contributed this field
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record FormFieldDataHolder(
    @Nullable String fieldTypeFqn,
    @NotNull String ownerFormTypeFqn
) {
    public FormFieldDataHolder {
        if (fieldTypeFqn != null && !fieldTypeFqn.startsWith("\\")) {
            throw new IllegalArgumentException("fieldTypeFqn must be normalized with a leading backslash");
        }

        if (!ownerFormTypeFqn.startsWith("\\")) {
            throw new IllegalArgumentException("ownerFormTypeFqn must be normalized with a leading backslash");
        }
    }
}
