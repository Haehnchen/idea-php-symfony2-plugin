package fr.adrienbrault.idea.symfony2plugin.twig.variable.globals;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigGlobalVariable {
    @NotNull
    private final String name;

    @NotNull
    private final String value;

    @NotNull
    private final TwigGlobalEnum twigGlobalEnum;


    TwigGlobalVariable(@NotNull String name, @NotNull String value, @NotNull TwigGlobalEnum twigGlobalEnum) {
        this.name = name;
        this.value = value;
        this.twigGlobalEnum = twigGlobalEnum;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getValue() {
        return value;
    }

    @NotNull
    public TwigGlobalEnum getTwigGlobalEnum() {
        return twigGlobalEnum;
    }
}
