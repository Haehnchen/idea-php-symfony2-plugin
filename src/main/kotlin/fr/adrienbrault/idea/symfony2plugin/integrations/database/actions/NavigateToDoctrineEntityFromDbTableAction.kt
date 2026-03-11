package fr.adrienbrault.idea.symfony2plugin.integrations.database.actions

import com.intellij.database.vfs.DatabaseElementVirtualFileImpl
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.util.OpenSourceUtil
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper
import fr.adrienbrault.idea.symfony2plugin.integrations.database.DoctrineEntityTableNameResolver

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class NavigateToDoctrineEntityFromDbTableAction : DumbAwareAction(
    "Go To Related Doctrine Entity",
    "Navigate to corresponding Doctrine entity for this table",
    Symfony2Icons.DOCTRINE
) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val enabled = project != null
            && virtualFile is DatabaseElementVirtualFileImpl
            && virtualFile.objectPath != null

        e.presentation.isEnabledAndVisible = enabled
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val tableName = getTableName(e) ?: return

        val navigatables = findEntityNavigatablesByTableName(project, tableName)
        if (navigatables.isEmpty()) return

        OpenSourceUtil.navigate(true, true, *navigatables.toTypedArray())
    }

    private fun getTableName(e: AnActionEvent): String? {
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) as? DatabaseElementVirtualFileImpl
            ?: return null
        return virtualFile.nameWithoutExtension
    }

    companion object {
        @JvmStatic
        fun findEntityNavigatablesByTableName(project: Project, tableName: String): List<Navigatable> {
            val navigatables = linkedSetOf<Navigatable>()

            for (model in EntityHelper.getModelClasses(project)) {
                val phpClass = model.phpClass
                if (!DoctrineEntityTableNameResolver.isTableMatch(phpClass, tableName)) continue

                val navigationElement = phpClass.navigationElement
                if (navigationElement is Navigatable) {
                    navigatables.add(navigationElement)
                } else {
                    navigatables.add(phpClass)
                }
            }

            return navigatables.toList()
        }
    }
}
