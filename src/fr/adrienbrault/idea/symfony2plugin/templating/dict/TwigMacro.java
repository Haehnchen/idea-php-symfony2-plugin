package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigMacro {
    @NotNull
    private String name;

    @NotNull
    private String template;

    @Nullable
    private String originalName;

    @Nullable
    private String parameter;

    public TwigMacro(@NotNull String name, @NotNull String template) {
        this.name = name;
        this.template = template;
    }

    public TwigMacro(@NotNull String name, @NotNull String template, @NotNull String originalName) {
        this(name, template);
        this.originalName = originalName;
    }

    @Nullable
    public String getOriginalName() {
        return originalName;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getTemplate() {
        return template;
    }

    @Nullable
    public String getParameter() {
        return parameter;
    }

    @NotNull
    public TwigMacro withParameter(@Nullable String parameter) {
        this.parameter = parameter;

        return this;
    }
}
