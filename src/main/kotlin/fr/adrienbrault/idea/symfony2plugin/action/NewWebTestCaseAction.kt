package fr.adrienbrault.idea.symfony2plugin.action

import com.intellij.ide.projectView.impl.ProjectRootsUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class NewWebTestCaseAction : AbstractNewPhpClassAction("WebTestCase", "Create WebTestCase Class") {
    override fun getClassNameSuffix() = "Test"

    override fun getTemplateName(project: Project, namespace: String, directory: PsiDirectory) = "web_test_case"

    class Shortcut : NewWebTestCaseAction() {
        override fun update(event: AnActionEvent) {
            val project = getEventProject(event)
            setStatus(
                event,
                project != null &&
                        Symfony2ProjectComponent.isEnabled(project) &&
                        NewFileActionUtil.getSelectedDirectoryFromAction(event)?.let {
                            ProjectRootsUtil.isInTestSource(it.virtualFile, project)
                        } == true
            )
        }
    }
}
