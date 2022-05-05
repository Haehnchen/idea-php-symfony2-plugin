package fr.adrienbrault.idea.symfony2plugin.asset;

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

    private String string = null;

    public AssetFile(@NotNull VirtualFile assetFile, @NotNull AssetEnum.Position assetPosition, @NotNull VirtualFile relativeFolder, @NotNull String prefix) {
        this(assetFile, assetPosition, relativeFolder);
        this.prefix = prefix;
    }

    public AssetFile(@NotNull VirtualFile assetFile, @NotNull AssetEnum.Position assetPosition, @NotNull VirtualFile relativeFolder) {
        this.assetFile = assetFile;
        this.assetPosition = assetPosition;
        this.relativeFolder = relativeFolder;
    }

    private AssetFile(@NotNull VirtualFile assetFile, @NotNull AssetEnum.Position assetPosition) {
        this.assetFile = assetFile;
        this.assetPosition = assetPosition;
    }

    public static AssetFile createVirtualManifestEntry(@NotNull VirtualFile manifestJson, @NotNull String name) {
        AssetFile myAssetFile = new AssetFile(manifestJson, AssetEnum.Position.Web);

        myAssetFile.string = name;

        return myAssetFile;
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
        return this.string != null
            ? this.string
            : this.prefix + VfsUtil.getRelativePath(assetFile, relativeFolder, '/');
    }
}
