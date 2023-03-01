package fr.adrienbrault.idea.symfony2plugin.form.dict;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public enum FormOptionEnum {
    UNKNOWN, DEFAULT, REQUIRED, DEFINED;

    public static FormOptionEnum getEnum(@NotNull String s) {

        return switch (s) {
            case "setDefault" -> DEFAULT;
            case "setRequired" -> REQUIRED;
            case "setDefined", "setOptional" -> DEFINED;
            default -> UNKNOWN;
        };

    }
}