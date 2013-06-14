package fr.adrienbrault.idea.symfony2plugin.templating.path;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class TwigPath {

    private String path;
    private String namespace = TwigPathIndex.MAIN;

    public TwigPath(String path) {
        this.path = path;
    }

    public TwigPath(String path, String namespace) {
        this.path = path;
        this.namespace = namespace;
    }

    public String getNamespace() {
        return namespace;
    }

    public boolean isGlobalNamespace() {
        return getNamespace().equals(TwigPathIndex.MAIN);
    }

    public String getPath() {
        return path;
    }

    @Nullable
    public VirtualFile getDirectory() {
        File file = new File(this.getPath());

        if(!file.exists()) {
            return null;
        }

        return VfsUtil.findFileByIoFile(file, true);
    }

}

