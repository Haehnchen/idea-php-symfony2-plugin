@file:Suppress("FunctionName", "unused")

package fr.adrienbrault.idea.symfony2plugin.mcp.toolset

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.mcpserver.project
import com.intellij.openapi.application.readAction
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.DoctrineEntityCollector
import fr.adrienbrault.idea.symfony2plugin.mcp.McpUtil
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
            mcpFail("Symfony plugin is not enabled for this project.")
        }

        McpUtil.checkToolEnabled(project, "list_doctrine_entities")

        return readAction {
            DoctrineEntityCollector(project).collect()
        }
    }
}
