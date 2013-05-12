package fr.adrienbrault.idea.symfony2plugin.util.dict;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopes;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.jetbrains.annotations.Nullable;

public class SymfonyBundle {

    protected PhpClass phpClass;

    public SymfonyBundle(PhpClass phpClass) {
        this.phpClass = phpClass;
    }

    public String getName() {
        return this.phpClass.getName();
    }

    @Nullable
    public PsiDirectory getDirectory() {
        if(null == this.phpClass.getContainingFile()) {
            return null;
        }

        PsiDirectory bundleDirectory = this.phpClass.getContainingFile().getContainingDirectory();
        if(null == bundleDirectory) {
            return null;
        }

        return bundleDirectory;
    }

    @Nullable
    public PsiDirectory getSubDirectory(String... names) {

        PsiDirectory currentDir = this.getDirectory();
        if(null == currentDir) {
            return null;
        }

        for(String name: names) {
            currentDir = currentDir.findSubdirectory(name);
            if(null == currentDir) {
                return null;
            }
        }

        return currentDir;
    }

    @Nullable
    public String getRelativePath(VirtualFile file) {

        PsiDirectory currentDir = this.getDirectory();
        if(null == currentDir) {
          return null;
        }

        return VfsUtil.getRelativePath(file, currentDir.getVirtualFile(), '/');
    }

    @Nullable
    public GlobalSearchScope getBundleSearchScope() {
        PsiDirectory currentDir = this.getDirectory();
        if(null == currentDir) {
            return null;
        }

        return GlobalSearchScopes.directoryScope(currentDir, true);
    }


}
