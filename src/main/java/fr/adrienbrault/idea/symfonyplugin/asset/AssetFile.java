package fr.adrienbrault.idea.symfonyplugin.asset;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AssetFile {
    @NotNull
    private VirtualFile assetFile;

    @NotNull
    private AssetEnum.Position assetPosition;

    @NotNull
    private VirtualFile relativeFolder;

    private String prefix = "";

    public AssetFile(@NotNull VirtualFile assetFile, @NotNull AssetEnum.Position assetPosition, @NotNull VirtualFile relativeFolder, @NotNull String prefix) {
        this(assetFile, assetPosition, relativeFolder);
        this.prefix = prefix;
    }

    public AssetFile(@NotNull VirtualFile assetFile, @NotNull AssetEnum.Position assetPosition, @NotNull VirtualFile relativeFolder) {
        this.assetFile = assetFile;
        this.assetPosition = assetPosition;
        this.relativeFolder = relativeFolder;
    }

    @NotNull
    public VirtualFile getFile() {
        return assetFile;
    }

    @NotNull
    public AssetEnum.Position getAssetPosition() {
        return assetPosition;
    }

    public String toString() {
        return this.prefix + VfsUtil.getRelativePath(assetFile, relativeFolder, '/');
    }
}
