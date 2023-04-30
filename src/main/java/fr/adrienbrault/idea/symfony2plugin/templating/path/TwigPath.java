package fr.adrienbrault.idea.symfony2plugin.templating.path;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigPath {
    @NotNull
    private final String path;

    @NotNull
    private String namespace = TwigUtil.MAIN;

    @NotNull
    private TwigUtil.NamespaceType namespaceType = TwigUtil.NamespaceType.ADD_PATH;

    private boolean enabled = true;
    private boolean customPath = false;

    @NotNull
    public TwigUtil.NamespaceType getNamespaceType() {
        return namespaceType;
    }

    public TwigPath(@NotNull String path) {
        this.path = path;
    }

    public TwigPath(@NotNull String path, @NotNull String namespace) {
        this.path = path;
        this.namespace = namespace;
    }

    public TwigPath(@NotNull String path, @NotNull String namespace, @NotNull TwigUtil.NamespaceType namespaceType, boolean customPath) {
        this(path, namespace, namespaceType);
        this.customPath = customPath;
    }

    public static TwigPath createClone(@NotNull TwigPath twigPath) {
        TwigPath newTwigPath = new TwigPath(
            twigPath.getPath(),
            twigPath.getNamespace(),
            twigPath.getNamespaceType(),
            twigPath.isCustomPath()
        );

        newTwigPath.setEnabled(twigPath.isEnabled());
        return newTwigPath;
    }

    public TwigPath(@NotNull String path, @NotNull String namespace, @NotNull TwigUtil.NamespaceType namespaceType) {
        this(path, namespace);
        this.namespaceType = namespaceType;
    }

    @NotNull
    public String getNamespace() {
        return namespace;
    }

    public boolean isGlobalNamespace() {
        return getNamespace().equals(TwigUtil.MAIN);
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

        return VfsUtil.getRelativePath(virtualFile, ProjectUtil.getProjectDir(project), '/');
    }

    @Nullable
    public VirtualFile getDirectory(@NotNull Project project) {
        if(!FileUtil.isAbsolute(path)) {
            return VfsUtil.findRelativeFile(path, ProjectUtil.getProjectDir(project));
        } else {
            VirtualFile fileByIoFile = VfsUtil.findFileByIoFile(new File(path), false);
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

        return VfsUtil.findFileByIoFile(file, false);
    }

    public boolean isCustomPath() {
        return customPath;
    }
}

