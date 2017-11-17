package fr.adrienbrault.idea.symfony2plugin.templating.path;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.text.Collator;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigPath implements Comparable<TwigPath> {
    @NotNull
    private String path;

    @NotNull
    private String namespace = TwigPathIndex.MAIN;

    @NotNull
    private TwigPathIndex.NamespaceType namespaceType = TwigPathIndex.NamespaceType.ADD_PATH;

    private boolean enabled = true;
    private boolean customPath = false;

    @NotNull
    public TwigPathIndex.NamespaceType getNamespaceType() {
        return namespaceType;
    }

    public TwigPath(@NotNull String path) {
        this.path = path;
    }

    public TwigPath(@NotNull String path, @NotNull String namespace) {
        this.path = path;
        this.namespace = namespace;
    }

    public TwigPath(@NotNull String path, @NotNull String namespace, @NotNull TwigPathIndex.NamespaceType namespaceType, boolean customPath) {
        this(path, namespace, namespaceType);
        this.customPath = customPath;
    }

    @Override
    public TwigPath clone() {
        try {
            super.clone();
        } catch (CloneNotSupportedException ignored) {
        }

        TwigPath twigPath = new TwigPath(this.getPath(), this.getNamespace(), this.getNamespaceType(), this.isCustomPath());
        twigPath.setEnabled(this.isEnabled());

        return twigPath;
    }

    public TwigPath(@NotNull String path, @NotNull String namespace, @NotNull TwigPathIndex.NamespaceType namespaceType) {
        this(path, namespace);
        this.namespaceType = namespaceType;
    }

    @NotNull
    public String getNamespace() {
        return namespace;
    }

    public boolean isGlobalNamespace() {
        return getNamespace().equals(TwigPathIndex.MAIN);
    }

    @Nullable
    public String getRelativePath(@NotNull Project project) {
        if(!FileUtil.isAbsolute(path)) {
            return path;
        }

        VirtualFile virtualFile = getDirectory();
        if(virtualFile == null) {
            return null;
        }

        return VfsUtil.getRelativePath(virtualFile, project.getBaseDir(), '/');
    }

    @Nullable
    public VirtualFile getDirectory(@NotNull Project project) {
        if(!FileUtil.isAbsolute(path)) {
            return VfsUtil.findRelativeFile(path, project.getBaseDir());
        } else {
            VirtualFile fileByIoFile = VfsUtil.findFileByIoFile(new File(path), true);
            if(fileByIoFile != null) {
                return fileByIoFile;
            }
        }

        return null;
    }

    @NotNull
    public String getPath() {
        return path;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Deprecated
    public TwigPath setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    @Nullable
    private VirtualFile getDirectory() {

        File file = new File(this.getPath());

        if(!file.exists()) {
            return null;
        }

        return VfsUtil.findFileByIoFile(file, true);
    }

    @Override
    public int compareTo(@NotNull TwigPath twigPath) {
        Collator collator = Collator.getInstance();
        collator.setStrength(Collator.SECONDARY);
        return collator.compare(this.getNamespace(), twigPath.getNamespace());
    }

    public boolean isCustomPath() {
        return customPath;
    }
}

