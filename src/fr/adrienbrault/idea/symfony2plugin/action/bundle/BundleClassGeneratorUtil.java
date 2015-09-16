package fr.adrienbrault.idea.symfony2plugin.action.bundle;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
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

            PhpClass aClass = PhpPsiUtil.findClass((PhpFile) psiFile, new Condition<PhpClass>() {
                @Override
                public boolean value(PhpClass phpClass) {
                    return new Symfony2InterfacesUtil().isInstanceOf(phpClass, "\\Symfony\\Component\\HttpKernel\\Bundle\\BundleInterface");
                }
            });

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

        final PsiDirectory initialBaseDir = directories[0];
        if (initialBaseDir == null) {
            return null;
        }

        if(initialBaseDir.getName().endsWith("Bundle")) {
            return initialBaseDir;
        }

        PsiDirectory parent = initialBaseDir.getParent();
        if(parent != null && parent.getName().endsWith("Bundle")) {
            return parent;
        }

        return null;
    }

}
