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
public class NewCommandAction extends AbstractNewPhpClassAction {
    public NewCommandAction() {
        super("Command", "Create Command Class");
    }

    @Override
    protected String getClassNameSuffix() {
        return "Command";
    }

    @Override
    protected String getTemplateName(@NotNull Project project, @NotNull String namespace, @NotNull PsiDirectory directory) {
        return NewFileActionUtil.guessCommandTemplateType(project, namespace);
    }

    @Override
    protected Map<String, String> getExtraVariables(@NotNull String className, @NotNull String namespace, @NotNull PsiDirectory directory) {
        String clazz = className.endsWith("Command")
            ? className.substring(0, className.length() - "Command".length())
            : className;
        String prefix = NewFileActionUtil.getCommandPrefix(directory);
        return Map.of("command_name", prefix + ":" + underscore(clazz));
    }

    public static class Shortcut extends NewCommandAction {
        @Override
        public void update(@NotNull AnActionEvent event) {
            this.setStatus(event, false);
            Project project = getEventProject(event);
            if (!Symfony2ProjectComponent.isEnabled(project)) {
                return;
            }

            this.setStatus(event, NewFileActionUtil.isInGivenDirectoryScope(event, "Command"));
        }
    }
}
