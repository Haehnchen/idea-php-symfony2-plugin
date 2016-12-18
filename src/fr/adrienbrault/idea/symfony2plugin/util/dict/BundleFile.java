package fr.adrienbrault.idea.symfony2plugin.util.dict;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class BundleFile {

    private SymfonyBundle symfonyBundle;
    private VirtualFile virtualFile;
    private Project project;

    public BundleFile(SymfonyBundle symfonyBundle, VirtualFile virtualFile, Project project) {
        this.symfonyBundle = symfonyBundle;
        this.virtualFile = virtualFile;
        this.project = project;
    }

    public SymfonyBundle getSymfonyBundle() {
        return symfonyBundle;
    }

    public VirtualFile getVirtualFile() {
        return virtualFile;
    }

    @Nullable
    public String getShortcutPath() {
        return this.getSymfonyBundle().getFileShortcut(this);
    }

    public Project getProject() {
        return project;
    }

}
