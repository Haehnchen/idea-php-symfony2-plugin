@file:Suppress("FunctionName", "unused")

package fr.adrienbrault.idea.symfony2plugin.mcp.toolset

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.project
import com.intellij.openapi.application.readAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper
import fr.adrienbrault.idea.symfony2plugin.mcp.McpUtil
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil
import kotlinx.coroutines.currentCoroutineContext

/**
 * MCP toolset for Doctrine entities.
 * Provides access to ORM entities configured in the Symfony project.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class DoctrineEntityMcpToolset : McpToolset {

    @McpTool
    @McpDescription("""
        Lists all Doctrine ORM entities in the project as CSV.

        Returns CSV format with columns: className,filePath
        - className: FQN of the entity class
        - filePath: Relative path from project root

        Example output:
        className,filePath
        App\Entity\User,src/Entity/User.php
    """)
    suspend fun list_doctrine_entities(): String {
        val project = currentCoroutineContext().project

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            throw IllegalStateException("Symfony plugin is not enabled for this project.")
        }

        McpUtil.checkToolEnabled(project, "list_doctrine_entities")

        return readAction {
            val entities = EntityHelper.getModelClasses(project)
            val projectDir = ProjectUtil.getProjectDir(project)

            val csv = StringBuilder("className,filePath\n")

            entities.forEach { entity ->
                val phpClass = entity.phpClass

                val filePath = phpClass.containingFile?.virtualFile?.let { virtualFile ->
                    projectDir?.let { dir ->
                        VfsUtil.getRelativePath(virtualFile, dir, '/')
                            ?: FileUtil.getRelativePath(dir.path, virtualFile.path, '/')
                    }
                } ?: ""

                csv.append("${escapeCsv(phpClass.fqn)},${escapeCsv(filePath)}\n")
            }

            csv.toString()
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
