package fr.adrienbrault.idea.symfony2plugin.dic.attribute.value;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface AttributeValueInterface {

    @Nullable
    String getString(@NotNull String key);

    @Nullable
    Boolean getBoolean(@NotNull String key);

    @NotNull
    String getString(@NotNull String key, String defaultValue);

    @NotNull
    Boolean getBoolean(@NotNull String key, Boolean defaultValue);
}
