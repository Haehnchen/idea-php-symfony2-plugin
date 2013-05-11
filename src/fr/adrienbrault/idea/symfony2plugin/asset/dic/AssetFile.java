package fr.adrienbrault.idea.symfony2plugin.asset.dic;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetEnum;

public class AssetFile {

    VirtualFile assetFile;
    AssetEnum.Position assetPosition;
    VirtualFile relativeFolder;
    String prefix = "";

    public AssetFile(VirtualFile assetFile, AssetEnum.Position assetPosition, VirtualFile relativeFolder, String prefix) {
        this(assetFile, assetPosition, relativeFolder);
        this.prefix = prefix;
    }

    public AssetFile(VirtualFile assetFile, AssetEnum.Position assetPosition, VirtualFile relativeFolder) {
        this.assetFile = assetFile;
        this.assetPosition = assetPosition;
        this.relativeFolder = relativeFolder;
    }

    public VirtualFile getFile() {
        return this.assetFile;
    }

    public AssetEnum.Position getAssetPosition() {
        return this.assetPosition;
    }

    public VirtualFile getRelativeFolder() {
        return this.relativeFolder;
    }

    public String toString() {
        return this.prefix + VfsUtil.getRelativePath(this.getFile(), this.getRelativeFolder(), '/');
    }

}
