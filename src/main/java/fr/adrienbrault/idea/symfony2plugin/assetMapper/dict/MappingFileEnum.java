package fr.adrienbrault.idea.symfony2plugin.assetMapper.dict;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public enum MappingFileEnum {
    IMPORTMAP, INSTALLED;

    public static MappingFileEnum fromString(@NotNull String text) {
        if (text.equalsIgnoreCase("importmap.php")) {
            return IMPORTMAP;
        }

        if (text.equalsIgnoreCase("installed.php")) {
            return INSTALLED;
        }

        throw new RuntimeException("invalid filename");
    }
}
