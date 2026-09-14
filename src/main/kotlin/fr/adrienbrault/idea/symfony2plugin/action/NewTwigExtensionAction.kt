package fr.adrienbrault.idea.symfony2plugin.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class NewTwigExtensionAction : AbstractNewPhpClassAction("TwigExtension", "Create TwigExtension Class") {
    override fun getTemplateName(project: Project, namespace: String, directory: PsiDirectory) =
        if (PhpElementsUtil.hasClassOrInterface(project, "\\Twig\\Attribute\\AsTwigFunction")) {
            "twig_extension_function_attribute"
        } else {
            "twig_extension"
        }

    class Shortcut : NewTwigExtensionAction() {
        override fun update(event: AnActionEvent) {
            setStatus(
                event,
                Symfony2ProjectComponent.isEnabled(getEventProject(event)) &&
                    NewFileActionUtil.isInGivenDirectoryScope(event, "Twig", "Extension")
            )
        }
    }
}
