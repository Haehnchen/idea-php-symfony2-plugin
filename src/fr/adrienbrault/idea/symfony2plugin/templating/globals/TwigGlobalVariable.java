package fr.adrienbrault.idea.symfony2plugin.templating.globals;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigGlobalVariable {

    private String name;
    private String value;
    private TwigGlobalEnum twigGlobalEnum;
    public TwigGlobalVariable(String name, String value, TwigGlobalEnum twigGlobalEnum) {
        this.name = name;
        this.value = value;
        this.twigGlobalEnum = twigGlobalEnum;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public TwigGlobalEnum getTwigGlobalEnum() {
        return twigGlobalEnum;
    }

}
