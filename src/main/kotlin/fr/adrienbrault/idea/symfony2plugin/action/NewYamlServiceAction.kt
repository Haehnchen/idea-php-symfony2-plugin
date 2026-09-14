package fr.adrienbrault.idea.symfony2plugin.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class NewYamlServiceAction : AbstractProjectDumbAwareAction(
    "Yaml Service",
    "Create new Yaml File",
    AllIcons.Nodes.DataTables
) {
    override fun update(event: AnActionEvent) {
        setStatus(
            event,
            Symfony2ProjectComponent.isEnabled(getEventProject(event)) &&
                NewFileActionUtil.getSelectedDirectoryFromAction(event) != null
        )
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        ServiceActionUtil.buildFile(event, project, "/fileTemplates/container.yml")
    }
}
