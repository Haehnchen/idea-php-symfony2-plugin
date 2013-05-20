package fr.adrienbrault.idea.symfony2plugin.util.dict;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
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

    public String getRelative(VirtualFile virtualFile) {
        PsiDirectory virtualDirectory =  this.getDirectory();
        if(virtualDirectory == null) {
            return null;
        }

        return VfsUtil.getRelativePath(virtualFile, virtualDirectory.getVirtualFile(), '/');
    }

}