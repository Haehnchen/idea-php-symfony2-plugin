package fr.adrienbrault.idea.symfony2plugin.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.util.StringUtils.underscore

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class NewControllerAction : AbstractNewPhpClassAction("Controller", "Create Controller Class") {
    override fun getClassNameSuffix() = "Controller"

    override fun getTemplateName(project: Project, namespace: String, directory: PsiDirectory): String =
        NewFileActionUtil.guessControllerTemplateType(project)

    override fun getExtraVariables(
        className: String,
        namespace: String,
        directory: PsiDirectory
    ): Map<String, String> {
        val clazz = if (className.endsWith("controller", ignoreCase = true)) {
            className.dropLast("controller".length)
        } else {
            className
        }
        return mapOf(
            "path" to "/${underscore(clazz).replace("_", "-")}",
            "template_path" to underscore(clazz)
        )
    }

    class Shortcut : NewControllerAction() {
        override fun update(event: AnActionEvent) {
            setStatus(
                event,
                Symfony2ProjectComponent.isEnabled(getEventProject(event)) &&
                        NewFileActionUtil.isInGivenDirectoryScope(event, "Controller")
            )
        }
    }
}
