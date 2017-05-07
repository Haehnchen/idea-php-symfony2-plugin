package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dict for "{% macro input(name, value, type, size) %}"
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigMacroTag implements TwigMacroTagInterface {

    @NotNull
    private final String name;

    @Nullable
    private final String parameters;

    /**
     * {% macro input(name, value, type, size) %}
     *
     * @param name macro name
     * @param parameters Raw parameter string (name, value, type, size)
     */
    public TwigMacroTag(@NotNull String name, @Nullable String parameters) {

        this.name = name;
        this.parameters = parameters;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @Nullable
    public String getParameters() {
        return parameters;
    }
}
