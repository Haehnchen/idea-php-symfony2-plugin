package fr.adrienbrault.idea.symfony2plugin.util.dict;

import com.intellij.psi.PsiDirectory;
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

}
