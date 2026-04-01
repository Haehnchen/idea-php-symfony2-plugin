package fr.adrienbrault.idea.symfony2plugin.action;

import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class NewKernelTestCaseAction extends AbstractNewPhpClassAction {
    public NewKernelTestCaseAction() {
        super("KernelTestCase", "Create KernelTestCase Class");
    }

    @Override
    protected String getClassNameSuffix() {
        return "Test";
    }

    @Override
    protected String getTemplateName(@NotNull Project project, @NotNull String namespace, @NotNull PsiDirectory directory) {
        return "kernel_test_case";
    }

    public static class Shortcut extends NewKernelTestCaseAction {
        @Override
        public void update(@NotNull AnActionEvent event) {
            this.setStatus(event, false);
            Project project = getEventProject(event);
            if (!Symfony2ProjectComponent.isEnabled(project)) {
                return;
            }

            PsiDirectory directory = NewFileActionUtil.getSelectedDirectoryFromAction(event);
            this.setStatus(event, directory != null && ProjectRootsUtil.isInTestSource(directory.getVirtualFile(), project));
        }
    }
}
