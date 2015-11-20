package fr.adrienbrault.idea.symfony2plugin.dic.attribute.value;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
abstract class AttributeValueAbstract implements AttributeValueInterface {

    @Override
    public Boolean getBoolean(@NotNull String key) {
        String value = getString(key);
        if(value == null) {
            return null;
        }

        if(value.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        } else if(value.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }

        return null;
    }

    @NotNull
    @Override
    public Boolean getBoolean(@NotNull String key, @NotNull Boolean defaultValue) {
        Boolean aBoolean = getBoolean(key);
        return aBoolean != null ? aBoolean : defaultValue;
    }

    @NotNull
    @Override
    public String getString(@NotNull String key, @NotNull String defaultValue) {
        String string = getString(key);
        return string != null ? string : defaultValue;
    }
}
