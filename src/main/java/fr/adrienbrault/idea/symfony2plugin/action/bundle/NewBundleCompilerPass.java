package fr.adrienbrault.idea.symfony2plugin.action.bundle;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.AbstractProjectDumbAwareAction;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PhpBundleFileFactory;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class NewBundleCompilerPass extends AbstractProjectDumbAwareAction {

    public NewBundleCompilerPass() {
        super("CompilerPass", "Create CompilerPass class", Symfony2Icons.SYMFONY);
    }

    public void update(AnActionEvent event) {
        Project project = getEventProject(event);
        if(project == null || !Symfony2ProjectComponent.isEnabled(project)) {
            this.setStatus(event, false);
            return;
        }

        this.setStatus(event, BundleClassGeneratorUtil.getBundleDirContext(event) != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {

        Project project = getEventProject(event);
        if(project == null) {
            this.setStatus(event, false);
            return;
        }

        PsiDirectory bundleDirContext = BundleClassGeneratorUtil.getBundleDirContext(event);
        if(bundleDirContext == null) {
            return;
        }

        final PhpClass phpClass = BundleClassGeneratorUtil.getBundleClassInDirectory(bundleDirContext);
        if(phpClass == null) {
            return;
        }

        new WriteCommandAction(project) {
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

}
