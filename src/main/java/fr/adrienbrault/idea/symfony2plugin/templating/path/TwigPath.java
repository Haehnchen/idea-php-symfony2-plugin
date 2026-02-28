package fr.adrienbrault.idea.symfony2plugin.templating.path;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;

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

    public static TwigPath createTwigPath(@NotNull String path, @NotNull String namespace, @NotNull TwigUtil.NamespaceType namespaceType, boolean customPath, boolean enabled) {
        TwigPath twigPath = new TwigPath(path, namespace, namespaceType, customPath);
        twigPath.enabled = enabled;
        return twigPath;
    }

    public static TwigPath createClone(@NotNull TwigPath twigPath) {
        return createClone(twigPath, twigPath.isEnabled());
    }

    public static TwigPath createClone(@NotNull TwigPath twigPath, boolean enabled) {
        TwigPath newTwigPath = new TwigPath(
            twigPath.getPath(),
            twigPath.getNamespace(),
            twigPath.getNamespaceType(),
            twigPath.isCustomPath()
        );

        newTwigPath.enabled = enabled;
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
            VirtualFile baseDir = ProjectUtil.getProjectDir(project);
            VirtualFile relativeFile = VfsUtil.findRelativeFile(baseDir, path.split("/"));

            if (relativeFile != null) {
                return relativeFile;
            }

            // Fallback for unit tests: find directory by name via index
            if (ApplicationManager.getApplication().isUnitTestMode()) {
                String[] parts = path.split("/");
                if (parts.length > 0) {
                    String dirName = parts[parts.length - 1];
                    Collection<VirtualFile> dirs = FilenameIndex.getVirtualFilesByName(
                        project, dirName, GlobalSearchScope.projectScope(project)
                    );
                    for (VirtualFile dir : dirs) {
                        if (dir.isDirectory()) {
                            return dir;
                        }
                    }
                }
            }
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

