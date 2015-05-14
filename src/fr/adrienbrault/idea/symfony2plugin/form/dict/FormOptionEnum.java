package fr.adrienbrault.idea.symfony2plugin.form.dict;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public enum FormOptionEnum {
    UNKNOWN, DEFAULT, REQUIRED, DEFINED;

    public static FormOptionEnum getEnum(@NotNull String s) {

        if("setDefault".equals(s)) {
            return DEFAULT;
        } else if("setRequired".equals(s) || "setOptional".equals(s)) {
            return REQUIRED;
        } else if("setDefined".equals(s)) {
            return DEFINED;
        }

        return UNKNOWN;
    }
}