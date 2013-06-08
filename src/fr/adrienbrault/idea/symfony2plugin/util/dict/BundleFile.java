package fr.adrienbrault.idea.symfony2plugin.util.dict;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

public class BundleFile {

    private SymfonyBundle symfonyBundle;
    private VirtualFile virtualFile;

    public BundleFile(SymfonyBundle symfonyBundle, VirtualFile virtualFile) {
        this.symfonyBundle = symfonyBundle;
        this.virtualFile = virtualFile;
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

}
