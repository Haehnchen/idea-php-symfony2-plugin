package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * UI metadata for a Symfony form field without holding PSI objects.
 *
 * @param fieldTypeFqn explicit normalized field form type FQN, or {@code null} when the builder call has no type parameter
 * @param ownerFormTypeFqn normalized FQN of the form type whose {@code buildForm()} contributed this field
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record FormDataHolder(
    @Nullable String fieldTypeFqn,
    @NotNull String ownerFormTypeFqn
) {
    public FormDataHolder {
        if (fieldTypeFqn != null && !fieldTypeFqn.startsWith("\\")) {
            throw new IllegalArgumentException("fieldTypeFqn must be normalized with a leading backslash");
        }

        if (!ownerFormTypeFqn.startsWith("\\")) {
            throw new IllegalArgumentException("ownerFormTypeFqn must be normalized with a leading backslash");
        }
    }
}
