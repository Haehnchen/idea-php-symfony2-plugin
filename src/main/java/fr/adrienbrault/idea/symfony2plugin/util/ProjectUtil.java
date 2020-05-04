package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * All project related function
 */
public class ProjectUtil {
    /**
     * Single project usage is deprecated which wraps the single project resolving
     *
     * RootManager should be taken as is module based and allows more then one root; for now change all is too much
     * You should provide a file context, with this help we can search for in which root its given; see replacement function
     */
    public static VirtualFile getProjectDir(@NotNull Project project) {
        return project.getBaseDir();
    }

    /**
     * This function should be use as a replaced, with given a context we know a possible project root
     */
    public static VirtualFile getProjectDir(@NotNull PsiElement context) {
        return getProjectDir(context.getProject());
    }
}
