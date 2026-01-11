@file:Suppress("FunctionName", "unused")

package fr.adrienbrault.idea.symfony2plugin.mcp.toolset

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.project
import com.intellij.openapi.application.readAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.php.PhpIndex
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.mcp.McpUtil
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil
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
            throw IllegalStateException("Symfony plugin is not enabled for this project.")
        }

        McpUtil.checkToolEnabled(project, "list_symfony_commands")

        return readAction {
            val commands = SymfonyCommandUtil.getCommands(project)
            val phpIndex = PhpIndex.getInstance(project)
            val projectDir = ProjectUtil.getProjectDir(project)

            val csv = StringBuilder("name,className,filePath\n")

            commands.forEach { command ->
                val filePath = phpIndex.getClassesByFQN(command.fqn).firstOrNull()
                    ?.containingFile
                    ?.virtualFile
                    ?.let { virtualFile ->
                        projectDir?.let { dir ->
                            VfsUtil.getRelativePath(virtualFile, dir, '/')
                                ?: FileUtil.getRelativePath(dir.path, virtualFile.path, '/')
                        }
                    } ?: ""

                csv.append("${escapeCsv(command.name)},${escapeCsv(command.fqn)},${escapeCsv(filePath)}\n")
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
