package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Metadata for a Twig root form view variable, for example {@code form}.
 * Example: {@code $this->render('edit.html.twig', ['form' => $form->createView()])}
 * exposes the root Twig variable {@code form}; its {@code formTypeFqns} point to the
 * Symfony form type classes that built that view.
 *
 * @param formTypeFqns normalized FQNs of Symfony form types that created the form view
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record FormViewDataHolder(@NotNull Set<String> formTypeFqns) {
    public FormViewDataHolder {
        formTypeFqns = toImmutableNormalizedSet(formTypeFqns);
    }

    @NotNull
    private static Set<String> toImmutableNormalizedSet(@NotNull Set<String> formTypeFqns) {
        Set<String> normalized = new LinkedHashSet<>();

        for (String formTypeFqn : formTypeFqns) {
            if (!formTypeFqn.startsWith("\\")) {
                throw new IllegalArgumentException("formTypeFqn must be normalized with a leading backslash");
            }

            normalized.add(formTypeFqn);
        }

        return Collections.unmodifiableSet(normalized);
    }
}
