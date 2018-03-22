package fr.adrienbrault.idea.symfony2plugin.util.dict;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyBundle {
    @NotNull
    final private PhpClass phpClass;

    @NotNull
    public PhpClass getPhpClass() {
        return this.phpClass;
    }

    @NotNull
    public String getNamespaceName() {
        return this.phpClass.getNamespaceName();
    }

    @NotNull
    public String getName() {
        return this.phpClass.getName();
    }

    public SymfonyBundle(@NotNull PhpClass phpClass) {
        this.phpClass = phpClass;
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
    public PsiDirectory getSubDirectory(@NotNull String... names) {
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
    public String getRelativePath(@NotNull VirtualFile file) {
        PsiDirectory currentDir = this.getDirectory();

        if(null == currentDir) {
          return null;
        }

        return VfsUtil.getRelativePath(file, currentDir.getVirtualFile(), '/');
    }

    public boolean isInBundle(@NotNull PhpClass phpClass) {
        return phpClass.getNamespaceName().startsWith(this.phpClass.getNamespaceName());
    }

    public boolean isInBundle(@NotNull PsiFile psiFile) {
        return isInBundle(psiFile.getVirtualFile());
    }

    public boolean isInBundle(@Nullable VirtualFile virtualFile) {
        if(virtualFile == null) {
            return false;
        }

        PsiDirectory psiDirectory =  this.getDirectory();
        return psiDirectory != null && VfsUtil.isAncestor(psiDirectory.getVirtualFile(), virtualFile, false);
    }

    @Nullable
    public VirtualFile getRelative(@NotNull String path) {
        PsiDirectory virtualDirectory =  this.getDirectory();
        if(virtualDirectory == null) {
            return null;
        }

       return VfsUtil.findRelativeFile(virtualDirectory.getVirtualFile(), path.split("/"));
    }

    @Nullable
    public String getRelative(@NotNull VirtualFile virtualFile) {
        PsiDirectory virtualDirectory =  this.getDirectory();
        if(virtualDirectory == null) {
            return null;
        }

        return VfsUtil.getRelativePath(virtualFile, virtualDirectory.getVirtualFile(), '/');
    }

    @Nullable
    public String getFileShortcut(@NotNull BundleFile bundleFile) {
        String relativePath = this.getRelative(bundleFile.getVirtualFile());
        if(relativePath == null) {
            return null;
        }

        return "@" + this.getName() + "/" + relativePath;
    }

    public boolean isTestBundle() {
        PsiDirectory directory = this.getDirectory();
        if(directory == null) {
            return false;
        }

        // @TODO: filter vendor, src before?
        return directory.getVirtualFile().toString().contains("/Tests/");
    }

    @Nullable
    public String getParentBundleName() {
        return PhpElementsUtil.getMethodReturnAsString(this.getPhpClass(), "getParent");
    }
}