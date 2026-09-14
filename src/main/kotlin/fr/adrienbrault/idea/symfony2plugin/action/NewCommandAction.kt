package fr.adrienbrault.idea.symfony2plugin.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.util.StringUtils.underscore

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class NewCommandAction : AbstractNewPhpClassAction("Command", "Create Command Class") {
    override fun getClassNameSuffix() = "Command"

    override fun getTemplateName(project: Project, namespace: String, directory: PsiDirectory): String =
        NewFileActionUtil.guessCommandTemplateType(project, namespace)

    override fun getExtraVariables(
        className: String,
        namespace: String,
        directory: PsiDirectory
    ): Map<String, String> {
        val clazz = className.removeSuffix("Command")
        val prefix = NewFileActionUtil.getCommandPrefix(directory)
        return mapOf("command_name" to "$prefix:${underscore(clazz)}")
    }

    class Shortcut : NewCommandAction() {
        override fun update(event: AnActionEvent) {
            setStatus(
                event,
                Symfony2ProjectComponent.isEnabled(getEventProject(event)) &&
                        NewFileActionUtil.isInGivenDirectoryScope(event, "Command")
            )
        }
    }
}
