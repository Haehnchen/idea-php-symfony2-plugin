package fr.adrienbrault.idea.symfony2plugin.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static fr.adrienbrault.idea.symfony2plugin.util.StringUtils.underscore;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class NewControllerAction extends AbstractNewPhpClassAction {
    public NewControllerAction() {
        super("Controller", "Create Controller Class");
    }

    @Override
    protected String getClassNameSuffix() {
        return "Controller";
    }

    @Override
    protected String getTemplateName(@NotNull Project project, @NotNull String namespace, @NotNull PsiDirectory directory) {
        return NewFileActionUtil.guessControllerTemplateType(project);
    }

    @Override
    protected Map<String, String> getExtraVariables(@NotNull String className, @NotNull String namespace, @NotNull PsiDirectory directory) {
        String clazz = className.toLowerCase().endsWith("controller")
            ? className.substring(0, className.length() - "controller".length())
            : className;
        return Map.of(
            "path", "/" + underscore(clazz).replace("_", "-"),
            "template_path", underscore(clazz)
        );
    }

    public static class Shortcut extends NewControllerAction {
        @Override
        public void update(@NotNull AnActionEvent event) {
            this.setStatus(event, false);
            Project project = getEventProject(event);
            if (!Symfony2ProjectComponent.isEnabled(project)) {
                return;
            }

            this.setStatus(event, NewFileActionUtil.isInGivenDirectoryScope(event, "Controller"));
        }
    }
}
