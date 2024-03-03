package fr.adrienbrault.idea.symfony2plugin.util.annotation;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AnnotationConfig {

    private final String name;
    private final String use;
    private ArrayList<AnnotationValue> values = new ArrayList<>();

    public AnnotationConfig(String name, String use) {
        this.name = name;
        this.use = use;
    }

    public AnnotationConfig(String name, String use, @Nullable ArrayList<AnnotationValue> values) {
        this(name, use);
        this.values = values;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public String getUse() {
        return use;
    }

    @Nullable
    public ArrayList<AnnotationValue> getValues() {
        return values;
    }

    public AnnotationConfig insertValue(String... name) {
        this.insertValue(AnnotationValue.Type.QuoteValue, name);
        return this;
    }

    public AnnotationConfig insertValue(AnnotationValue.Type type, String... name) {
        for(String nameValue: name) {
            this.values.add(new AnnotationValue(nameValue, type));
        }

        return this;
    }

}
