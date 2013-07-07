package fr.adrienbrault.idea.symfony2plugin.templating.dict;

public class TwigMacro {

    private String name;
    private String template;

    public TwigMacro(String name, String template) {
        this.name = name;
        this.template = template;
    }

    public String getName() {
        return name;
    }

    public String getTemplate() {
        return template;
    }

}
