package fr.adrienbrault.idea.symfony2plugin.assetMapper.dict

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
enum class MappingFileEnum {
    IMPORTMAP, INSTALLED;

    companion object {
        @JvmStatic
        fun fromString(text: String): MappingFileEnum {
            if (text.equals("importmap.php", ignoreCase = true)) {
                return IMPORTMAP
            }

            if (text.equals("installed.php", ignoreCase = true)) {
                return INSTALLED
            }

            throw RuntimeException("invalid filename")
        }
    }
}
