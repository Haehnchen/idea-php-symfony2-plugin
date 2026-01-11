package fr.adrienbrault.idea.symfony2plugin.mcp

import com.intellij.openapi.project.Project

/**
 * Utility class for MCP (Model Context Protocol) tools
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
object McpUtil {

    /**
     * Checks if MCP tools are globally enabled and if a specific tool is enabled.
     * Throws IllegalStateException if disabled.
     *
     * @param project The current project
     * @param toolId The ID of the MCP tool to check
     * @throws IllegalStateException if MCP is disabled globally or the specific tool is disabled
     */
    fun checkToolEnabled(project: Project, toolId: String) {
        val settings = McpSettings.getInstance(project)

        if (!settings.mcpEnabled) {
            throw IllegalStateException(
                "MCP tools are disabled for this project. " +
                "Enable them in Settings > Languages & Frameworks > PHP > Symfony > MCP Tools"
            )
        }

        val toolSettings = settings.mcpToolSettings
        if (toolSettings != null) {
            val toolSetting = toolSettings.find { it.toolId == toolId }
            if (toolSetting != null && !toolSetting.isEnabled) {
                throw IllegalStateException(
                    "The MCP tool '$toolId' is disabled for this project. " +
                    "Enable it in Settings > Languages & Frameworks > PHP > Symfony > MCP Tools"
                )
            }
        }
    }
}
