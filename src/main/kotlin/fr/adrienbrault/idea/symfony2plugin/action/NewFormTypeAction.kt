package fr.adrienbrault.idea.symfony2plugin.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class NewFormTypeAction : AbstractNewPhpClassAction("Form", "Create FormType Class") {
    override fun getTemplateName(project: Project, namespace: String, directory: PsiDirectory) = "form_type"

    class Shortcut : NewFormTypeAction() {
        override fun update(event: AnActionEvent) {
            setStatus(
                event,
                Symfony2ProjectComponent.isEnabled(getEventProject(event)) &&
                    NewFileActionUtil.isInGivenDirectoryScope(event, "Form", "Type")
            )
        }
    }
}
