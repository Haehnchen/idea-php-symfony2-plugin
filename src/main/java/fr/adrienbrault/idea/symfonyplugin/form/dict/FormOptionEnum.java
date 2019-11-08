package fr.adrienbrault.idea.symfonyplugin.form.dict;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public enum FormOptionEnum {
    UNKNOWN, DEFAULT, REQUIRED, DEFINED;

    public static FormOptionEnum getEnum(@NotNull String s) {

        switch (s) {
            case "setDefault":
                return DEFAULT;
            case "setRequired":
                return REQUIRED;
            case "setDefined":
            case "setOptional":
                return DEFINED;
        }

        return UNKNOWN;
    }
}