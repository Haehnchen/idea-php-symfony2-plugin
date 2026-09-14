package fr.adrienbrault.idea.symfony2plugin.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.php.PhpIcons
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class NewPhpServiceAction : AbstractProjectDumbAwareAction(
    "PHP Service",
    "Create new PHP File",
    PhpIcons.PHP_FILE
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
        ServiceActionUtil.buildFile(event, project, "/fileTemplates/container.php")
    }
}
