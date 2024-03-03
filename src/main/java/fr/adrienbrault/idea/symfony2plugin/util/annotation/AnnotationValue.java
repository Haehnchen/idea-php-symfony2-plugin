package fr.adrienbrault.idea.symfony2plugin.util.annotation;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AnnotationValue {

    private final String name;
    private final Type type;

    public AnnotationValue(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    public AnnotationValue(String name) {
        this.name = name;
        this.type = Type.QuoteValue;
    }

    public String getName() {
        return this.name;
    }

    public Type getType() {
        return this.type;
    }

    public enum Type {
        QuoteValue, Array
    }

}
