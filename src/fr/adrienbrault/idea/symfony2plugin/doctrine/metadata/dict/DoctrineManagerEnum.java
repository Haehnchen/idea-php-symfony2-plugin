package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public enum DoctrineManagerEnum {
    ORM, COUCHDB, MONGODB, DOCUMENT, ODM;

    @Nullable
    public static DoctrineManagerEnum getEnumFromString(@Nullable String text) {
        if(text == null) {
            return null;
        }

        try {
            return Enum.valueOf(DoctrineManagerEnum.class, text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
