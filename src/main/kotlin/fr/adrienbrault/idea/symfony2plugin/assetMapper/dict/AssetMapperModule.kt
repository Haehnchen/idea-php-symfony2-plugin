package fr.adrienbrault.idea.symfony2plugin.assetMapper.dict

import com.intellij.openapi.vfs.VirtualFile

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@JvmRecord
data class AssetMapperModule(
    val sourceType: MappingFileEnum,
    val sourceFile: VirtualFile,
    val key: String,
    val path: String?,
    val url: String?,
    val version: String?,
    val entrypoint: Boolean?,
    val type: String?,
)
