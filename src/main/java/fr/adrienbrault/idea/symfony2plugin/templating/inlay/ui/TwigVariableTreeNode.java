package fr.adrienbrault.idea.symfony2plugin.templating.inlay.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Data model for a node in the Twig variable tree popup.
 * <p>
 * Level 1 (variable): parentVariable == null
 * Level 2 (property): parentVariable == variable name
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record TwigVariableTreeNode(String name, Set<String> types, @Nullable String parentVariable) {

    public TwigVariableTreeNode(@NotNull String name, @NotNull Set<String> types, @Nullable String parentVariable) {
        this.name = name;
        this.types = types;
        this.parentVariable = parentVariable;
    }

    public boolean isPropertyNode() {
        return parentVariable != null;
    }

    public String getTypeDisplay() {
        return types.stream()
            .filter(t -> !t.isBlank())
            .map(t -> {
                String clean = t.startsWith("\\") ? t.substring(1) : t;
                int last = clean.lastIndexOf('\\');
                return last >= 0 ? clean.substring(last + 1) : clean;
            })
            .distinct()
            .collect(Collectors.joining("|"));
    }

    public boolean isBool() {
        return types.stream().anyMatch(t -> t.equalsIgnoreCase("bool") || t.equalsIgnoreCase("\\bool"));
    }

    public boolean isList() {
        return types.stream().anyMatch(t -> t.endsWith("[]"));
    }
}
