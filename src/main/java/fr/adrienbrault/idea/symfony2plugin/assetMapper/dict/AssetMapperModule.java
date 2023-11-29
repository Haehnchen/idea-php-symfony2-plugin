package fr.adrienbrault.idea.symfony2plugin.assetMapper.dict;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record AssetMapperModule(
    @NotNull MappingFileEnum sourceType,
    @NotNull VirtualFile sourceFile,
    @NotNull String key,
    @Nullable String path,
    @Nullable String url,
    @Nullable String version,
    @Nullable Boolean entrypoint,
    @Nullable String type
) {
}
