package fr.adrienbrault.idea.symfony2plugin.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class NewYamlServiceAction extends AbstractProjectDumbAwareAction {

    public NewYamlServiceAction() {
        super("Yaml Service", "Create new Yaml File", AllIcons.Nodes.DataTables);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        final Project project = event.getData(PlatformDataKeys.PROJECT);
        ServiceActionUtil.buildFile(event, project, "/resources/fileTemplates/container.yml");
    }

}
