@file:Suppress("FunctionName", "unused")

package fr.adrienbrault.idea.symfony2plugin.mcp.toolset

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.mcpserver.project
import com.intellij.openapi.application.readAction
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.mcp.McpUtil
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyCommandCollector
import kotlinx.coroutines.currentCoroutineContext

/**
 * MCP toolset for Symfony console commands.
 * Provides access to console commands configured in the Symfony project.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class CommandMcpToolset : McpToolset {

    @McpTool
    @McpDescription("""
        Lists all Symfony console commands available in the project as CSV.

        Returns CSV format with columns: name,className,filePath
        - name: Command name (e.g., cache:clear)
        - className: FQN of implementing class
        - filePath: Relative path from project root

        Example output:
        name,className,filePath
        cache:clear,\App\Command\CacheClearCommand,src/Command/CacheClearCommand.php
    """)
    suspend fun list_symfony_commands(): String {
        val project = currentCoroutineContext().project

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            mcpFail("Symfony plugin is not enabled for this project.")
        }

        McpUtil.checkToolEnabled(project, "list_symfony_commands")

        return readAction {
            SymfonyCommandCollector(project).collect()
        }
    }
}
