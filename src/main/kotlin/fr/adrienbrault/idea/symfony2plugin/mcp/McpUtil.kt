package fr.adrienbrault.idea.symfony2plugin.mcp

import com.intellij.mcpserver.mcpFail
import com.intellij.openapi.project.Project

/**
 * Utility class for MCP (Model Context Protocol) tools
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
object McpUtil {

    /**
     * Checks if MCP tools are enabled at the application level.
     * Also checks if a specific tool is enabled at the project level.
     * Calls mcpFail if disabled.
     *
     * Note: Application-level visibility (global column) is checked at tool registration time,
     * so this method primarily checks project-level per-tool settings.
     *
     * @param project The current project
     * @param toolId The ID of the MCP tool to check
     */
    fun checkToolEnabled(project: Project, toolId: String) {
        // Check application-level master enable
        val appSettings = McpApplicationSettings.getInstance()
        if (!appSettings.isMcpEnabled) {
            mcpFail(
                "MCP tools are globally disabled. " +
                "Enable them in Settings > Languages & Frameworks > PHP > Symfony > MCP Tools"
            )
        }

        // Check project-level per-tool settings
        val settings = McpSettings.getInstance(project)
        val toolSettings = settings.mcpToolSettings
        if (toolSettings != null) {
            val toolSetting = toolSettings.find { it.toolId == toolId }
            if (toolSetting != null && !toolSetting.isEnabled) {
                mcpFail(
                    "The MCP tool '$toolId' is disabled for this project. " +
                    "Enable it in Settings > Languages & Frameworks > PHP > Symfony > MCP Tools"
                )
            }
        }
    }

    /**
     * Checks if a tool is globally visible (application-level setting)
     * This is used by the tools provider to filter tools at registration time.
     *
     * @param toolId The ID of the MCP tool to check
     * @return true if the tool should be exposed to the MCP server
     */
    fun isToolGloballyVisible(toolId: String): Boolean {
        return McpApplicationSettings.getInstance().isToolGloballyVisible(toolId)
    }
}
