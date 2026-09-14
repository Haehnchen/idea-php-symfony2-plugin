package fr.adrienbrault.idea.symfony2plugin.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlFile
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.PhpClass
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.action.ui.SymfonyCreateService
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import org.jetbrains.yaml.psi.YAMLFile

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class SymfonyContainerServiceBuilder : DumbAwareAction(
    "Create Service",
    "Generate a new Service definition from class name",
    Symfony2Icons.SYMFONY
) {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val psiFile = if (Symfony2ProjectComponent.isEnabled(event.project)) {
            findPhpClass(event)?.first
        } else {
            null
        }
        setStatus(event, psiFile is YAMLFile || psiFile is XmlFile || psiFile is PhpFile)
    }

    private fun setStatus(event: AnActionEvent, status: Boolean) {
        event.presentation.isVisible = status
        event.presentation.isEnabled = status
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(PlatformDataKeys.EDITOR)

        val applicationWindow = IdeHelper.getWindowComponentFromProject(project) ?: return

        val (psiFile, phpClass) = findPhpClass(event) ?: return
        if (phpClass == null) {
            SymfonyCreateService.create(applicationWindow, project, psiFile, editor)
            return
        }

        SymfonyCreateService.create(applicationWindow, project, psiFile, phpClass, editor)
    }

    private fun findPhpClass(event: AnActionEvent): Pair<PsiFile, PhpClass?>? {
        val psiFile = event.getData(PlatformDataKeys.PSI_FILE)
        if (psiFile !is YAMLFile && psiFile !is XmlFile && psiFile !is PhpFile) {
            return null
        }

        // menu item like ProjectView
        if (event.place == "ProjectViewPopup") {
            // fins php class on scope
            val phpClass = (psiFile as? PhpFile)?.let(PhpElementsUtil::getFirstClassFromFile)
            return Pair(psiFile, phpClass)
        }

        // directly got the class
        val psiElement = event.getData(PlatformDataKeys.PSI_ELEMENT)
        if (psiElement is PhpClass) {
            return Pair(psiFile, psiElement)
        }

        // click inside class
        val phpClass = (psiFile as? PhpFile)?.let(PhpElementsUtil::getFirstClassFromFile)
        return Pair(psiFile, phpClass)
    }
}
