package fr.adrienbrault.idea.symfony2plugin.action;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import de.espend.idea.php.annotation.util.IdeUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PhpBundleFileFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NewBundleCompilerPass extends AbstractProjectDumbAwareAction {

    public NewBundleCompilerPass() {
        super("CompilerPass", "Create CompilerPass class", PhpIcons.CLASS);
    }

    public void update(AnActionEvent event) {
        this.setStatus(event, getBundleDirContext(event) != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {

        PsiDirectory bundleDirContext = getBundleDirContext(event);
        if(bundleDirContext == null) {
            return;
        }

        final PhpClass phpClass = getBundleClassInDirectory(bundleDirContext);
        if(phpClass == null) {
            return;
        }

        new WriteCommandAction(event.getData(PlatformDataKeys.PROJECT)) {
            @Override
            protected void run(@NotNull Result result) throws Throwable {
                PsiElement psiFile = PhpBundleFileFactory.invokeCreateCompilerPass(phpClass, null);
                if(psiFile != null) {
                    new OpenFileDescriptor(getProject(), psiFile.getContainingFile().getVirtualFile(), 0).navigate(true);
                }
            }

            @Override
            public String getGroupID() {
                return "Create CompilerClass";
            }
        }.execute();

    }

    @Nullable
    private PhpClass getBundleClassInDirectory(PsiDirectory bundleDirContext) {

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
    private PsiDirectory getBundleDirContext(@NotNull AnActionEvent event) {

        Project project = event.getData(PlatformDataKeys.PROJECT);

        if (project == null || !Symfony2ProjectComponent.isEnabled(project)) {
            return null;
        }

        DataContext dataContext = event.getDataContext();
        IdeView view = LangDataKeys.IDE_VIEW.getData(dataContext);
        if (view == null) {
            return null;
        }

        final PsiDirectory initialBaseDir = view.getOrChooseDirectory();
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
