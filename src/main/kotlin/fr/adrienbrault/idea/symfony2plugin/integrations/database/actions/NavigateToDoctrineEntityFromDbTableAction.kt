package fr.adrienbrault.idea.symfony2plugin.integrations.database.actions

import com.intellij.database.vfs.DatabaseElementVirtualFileImpl
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.util.OpenSourceUtil
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil

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

        val navigatables = DoctrineMetadataUtil.findEntityByTableName(project, tableName)
        if (navigatables.isEmpty()) return

        OpenSourceUtil.navigate(true, true, *navigatables.toTypedArray())
    }

    private fun getTableName(e: AnActionEvent): String? {
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) as? DatabaseElementVirtualFileImpl
            ?: return null
        return virtualFile.nameWithoutExtension
    }
}
