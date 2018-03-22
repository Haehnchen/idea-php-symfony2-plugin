package fr.adrienbrault.idea.symfony2plugin.action.bundle;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.util.FilesystemUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class BundleClassGeneratorUtil {

    @Nullable
    public static PhpClass getBundleClassInDirectory(@NotNull PsiDirectory bundleDirContext) {

        for (PsiFile psiFile : bundleDirContext.getFiles()) {

            if(!(psiFile instanceof PhpFile)) {
                continue;
            }

            PhpClass aClass = PhpPsiUtil.findClass((PhpFile) psiFile, phpClass ->
                PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\HttpKernel\\Bundle\\BundleInterface")
            );

            if(aClass != null) {
                return aClass;
            }

        }

        return null;
    }


    @Nullable
    public static PsiDirectory getBundleDirContext(@NotNull AnActionEvent event) {

        Project project = event.getData(PlatformDataKeys.PROJECT);
        if (project == null) {
            return null;
        }

        DataContext dataContext = event.getDataContext();
        IdeView view = LangDataKeys.IDE_VIEW.getData(dataContext);
        if (view == null) {
            return null;
        }

        PsiDirectory[] directories = view.getDirectories();
        if(directories.length == 0) {
            return null;
        }

        return FilesystemUtil.findParentBundleFolder(directories[0]);
    }

}
