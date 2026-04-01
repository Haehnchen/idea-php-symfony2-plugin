package fr.adrienbrault.idea.symfony2plugin.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class NewTwigExtensionAction extends AbstractNewPhpClassAction {
    public NewTwigExtensionAction() {
        super("TwigExtension", "Create TwigExtension Class");
    }

    @Override
    protected String getTemplateName(@NotNull Project project, @NotNull String namespace, @NotNull PsiDirectory directory) {
        if (PhpElementsUtil.hasClassOrInterface(project, "\\Twig\\Attribute\\AsTwigFunction")) {
            return "twig_extension_function_attribute";
        }
        return "twig_extension";
    }

    public static class Shortcut extends NewTwigExtensionAction {
        @Override
        public void update(@NotNull AnActionEvent event) {
            this.setStatus(event, false);
            Project project = getEventProject(event);
            if (!Symfony2ProjectComponent.isEnabled(project)) {
                return;
            }

            this.setStatus(event, NewFileActionUtil.isInGivenDirectoryScope(event, "Twig", "Extension"));
        }
    }
}
