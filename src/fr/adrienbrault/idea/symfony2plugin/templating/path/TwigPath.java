package fr.adrienbrault.idea.symfony2plugin.templating.path;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.text.Collator;

public class TwigPath implements Comparable<TwigPath> {

    private String path;
    private String namespace = TwigPathIndex.MAIN;
    private boolean enabled = true;
    private boolean customPath = false;

    public TwigPathIndex.NamespaceType getNamespaceType() {
        return namespaceType;
    }

    private TwigPathIndex.NamespaceType namespaceType = TwigPathIndex.NamespaceType.ADD_PATH;

    public TwigPath(String path) {
        this.path = path;
    }

    public TwigPath(String path, String namespace) {
        this.path = path;
        this.namespace = namespace;
    }

    public TwigPath(String path, String namespace, TwigPathIndex.NamespaceType namespaceType, boolean customPath) {
        this(path, namespace, namespaceType);
        this.customPath = customPath;
    }

    public TwigPath(String path, String namespace, TwigPathIndex.NamespaceType namespaceType) {
        this(path, namespace);
        this.namespaceType = namespaceType;
    }

    public String getNamespace() {
        return namespace;
    }

    public boolean isGlobalNamespace() {
        return getNamespace().equals(TwigPathIndex.MAIN);
    }

    @Nullable
    public String getRelativePath(Project project) {

        if(this.isCustomPath()) {
            return this.getPath();
        }

        VirtualFile virtualFile = this.getDirectory();
        if(virtualFile == null) {
            return null;
        }

        return VfsUtil.getRelativePath(virtualFile, project.getBaseDir(), '/');
    }

    @Nullable
    public VirtualFile getDirectory(Project project) {
        String relativePath = this.getRelativePath(project);
        if(relativePath == null) {
            return null;
        }

        return VfsUtil.findRelativeFile(relativePath, project.getBaseDir());
    }

    public String getPath() {
        return path;
    }

    public boolean isEnabled() {
        return enabled;
    }

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
    public int compareTo(TwigPath twigPath) {
        Collator collator = Collator.getInstance();
        collator.setStrength(Collator.SECONDARY);
        return collator.compare(this.getNamespace(), twigPath.getNamespace());
    }

    public boolean isCustomPath() {
        return customPath;
    }
}

