package fr.adrienbrault.idea.symfonyplugin.completion.constant;

import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EnumConstantFilter {

    private String instance;
    private String field;
    private String[] stringValues;

    public EnumConstantFilter(String instance, String field) {
        this.instance = instance;
        this.field = field;
    }

    public EnumConstantFilter(String[] stringValues) {
        this.stringValues = stringValues;
    }

    @Nullable
    public String getInstance() {
        return instance;
    }

    @Nullable
    public String getField() {
        return field;
    }

    @Nullable
    public String[] getStringValues() {
        return stringValues;
    }

}

