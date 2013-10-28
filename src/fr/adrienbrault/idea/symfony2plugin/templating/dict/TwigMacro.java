package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import org.jetbrains.annotations.Nullable;

public class TwigMacro {

    private String name;
    private String template;
    private String originalName;

    public TwigMacro(String name, String template) {
        this.name = name;
        this.template = template;
    }

    public TwigMacro(String name, String template, String originalName) {
        this(name, template);
        this.originalName = originalName;
    }

    @Nullable
    public String getOriginalName() {
        return originalName;
    }

    public String getName() {
        return name;
    }

    public String getTemplate() {
        return template;
    }

}
