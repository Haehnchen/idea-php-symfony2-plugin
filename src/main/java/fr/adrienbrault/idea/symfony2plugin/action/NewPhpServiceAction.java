package fr.adrienbrault.idea.symfony2plugin.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.jetbrains.php.PhpIcons;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class NewPhpServiceAction extends AbstractProjectDumbAwareAction {

    public NewPhpServiceAction() {
        super("PHP Service", "Create new PHP File", PhpIcons.PHP_FILE);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        final Project project = event.getData(PlatformDataKeys.PROJECT);
        ServiceActionUtil.buildFile(event, project, "/fileTemplates/container.php");
    }
}
