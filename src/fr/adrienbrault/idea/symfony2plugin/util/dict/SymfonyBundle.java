package fr.adrienbrault.idea.symfony2plugin.util.dict;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopes;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.jetbrains.annotations.Nullable;

public class SymfonyBundle {

    protected PhpClass phpClass;

    public SymfonyBundle(PhpClass phpClass) {
        this.phpClass = phpClass;
    }

    public PhpClass getPhpClass() {
        return this.phpClass;
    }

    public String getNamespaceName() {
        return this.phpClass.getNamespaceName();
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
    public VirtualFile getVirtualDirectory() {
        PsiDirectory psiDirectory = this.getDirectory();
        if(psiDirectory == null) {
            return null;
        }

        return psiDirectory.getVirtualFile();
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

    public boolean isInBundle(PhpClass phpClass) {
        return phpClass.getNamespaceName().startsWith(this.phpClass.getNamespaceName());
    }

    public boolean isInBundle(PsiFile psiFile) {
        VirtualFile virtualFile = psiFile.getVirtualFile();
        if(virtualFile == null) {
            return false;
        }

        PsiDirectory psiDirectory =  this.getDirectory();
        return psiDirectory != null && VfsUtil.isAncestor(psiDirectory.getVirtualFile(), virtualFile, false);

    }

    @Nullable
    public VirtualFile getRelative(String path) {
        PsiDirectory virtualDirectory =  this.getDirectory();
        if(virtualDirectory == null) {
            return null;
        }

       return VfsUtil.findRelativeFile(virtualDirectory.getVirtualFile(), path.split("/"));

    }

    @Nullable
    public String getRelative(VirtualFile virtualFile) {
        PsiDirectory virtualDirectory =  this.getDirectory();
        if(virtualDirectory == null) {
            return null;
        }

        return VfsUtil.getRelativePath(virtualFile, virtualDirectory.getVirtualFile(), '/');
    }

    @Nullable
    public String getRelative(VirtualFile virtualFile, boolean stripExtension) {
        String relativePath =  this.getRelative(virtualFile);
        if(relativePath == null) {
            return null;
        }

        if(!stripExtension) {
            return relativePath;
        }

        int bla = relativePath.lastIndexOf(".");
        if(bla == -1) {
            return null;
        }

        return relativePath.substring(0, bla);
    }

    @Nullable
    public String getFileShortcut(BundleFile bundleFile) {
        String relativePath = this.getRelative(bundleFile.getVirtualFile());
        if(relativePath == null) {
            return null;
        }

        return "@" + this.getName() + "/" + relativePath;
    }

}