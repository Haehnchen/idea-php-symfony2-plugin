package fr.adrienbrault.idea.symfony2plugin.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class NewXmlServiceAction : AbstractProjectDumbAwareAction(
    "Xml Service",
    "Create new Xml File",
    AllIcons.FileTypes.Xml
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
        ServiceActionUtil.buildFile(event, project, "/fileTemplates/container.xml")
    }
}
