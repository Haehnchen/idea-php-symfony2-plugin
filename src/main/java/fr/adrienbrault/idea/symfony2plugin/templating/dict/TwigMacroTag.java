package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dict for "{% macro input(name, value, type, size) %}"
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record TwigMacroTag(@NotNull String name, @Nullable String parameters) implements TwigMacroTagInterface {
    /**
     * {% macro input(name, value, type, size) %}
     *
     * @param name       macro name
     * @param parameters Raw parameter string (name, value, type, size)
     */
    public TwigMacroTag {}
}
