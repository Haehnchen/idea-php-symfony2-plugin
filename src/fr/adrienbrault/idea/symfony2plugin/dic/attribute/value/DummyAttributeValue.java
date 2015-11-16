package fr.adrienbrault.idea.symfony2plugin.dic.attribute.value;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DummyAttributeValue implements AttributeValueInterface {
    @Nullable
    @Override
    public String getString(@NotNull String key) {
        return null;
    }
}
