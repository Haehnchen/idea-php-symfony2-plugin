package fr.adrienbrault.idea.symfony2plugin.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class NewCompilerPassAction extends AbstractNewPhpClassAction {
    public NewCompilerPassAction() {
        super("CompilerPass", "Create CompilerPass Class");
    }

    @Override
    protected String getTemplateName(@NotNull Project project, @NotNull String namespace, @NotNull PsiDirectory directory) {
        return "compiler_pass";
    }

    public static class Shortcut extends NewCompilerPassAction {
        @Override
        public void update(@NotNull AnActionEvent event) {
            this.setStatus(event, false);
            Project project = getEventProject(event);
            if (!Symfony2ProjectComponent.isEnabled(project)) {
                return;
            }

            this.setStatus(event, NewFileActionUtil.isInGivenDirectoryScope(event, "Compiler", "DependencyInjection", "CompilerPass"));
        }
    }
}
