package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dict for "{% macro input(name, value, type, size) %}"
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface TwigMacroTagInterface {
    @NotNull
    String name();

    @Nullable
    String parameters();
}
