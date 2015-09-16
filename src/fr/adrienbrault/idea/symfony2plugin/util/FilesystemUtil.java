package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.psi.PsiDirectory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FilesystemUtil {

    @Nullable
    public static PsiDirectory findParentBundleFolder(@NotNull PsiDirectory directory) {

        // self click
        if(directory.isDirectory() && directory.getName().endsWith("Bundle")) {
            return directory;
        }

        for (PsiDirectory parent = directory.getParent(); parent != null; parent = parent.getParent()) {
            if(parent.isDirectory() && parent.getName().endsWith("Bundle")) {
                return parent;
            }
        }

        return null;
    }

}
