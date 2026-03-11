package fr.adrienbrault.idea.symfony2plugin.mcp

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Application-level settings for MCP (Model Context Protocol) tools.
 * Controls which tools are globally visible to the MCP server.
 * Tools disabled here are completely hidden from the MCP server.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@State(
    name = "SymfonyMcpApplicationSettings",
    storages = [Storage("symfony-mcp-application.xml")]
)
class McpApplicationSettings : PersistentStateComponent<McpApplicationSettings> {

    /**
     * Master enable/disable for all MCP tools globally.
     * When disabled, no tools are exposed to the MCP server.
     */
    @JvmField
    var mcpEnabled: Boolean = true

    /**
     * Map of toolId -> enabled (globally visible to MCP server)
     * Tools not in this map are considered enabled by default
     */
    var toolVisibility: MutableMap<String, Boolean> = HashMap()

    /**
     * Check if a tool is globally visible to the MCP server.
     * Returns false if MCP is globally disabled, or if the specific tool is disabled.
     *
     * @param toolId The tool ID to check
     * @return true if the tool should be exposed to MCP server
     */
    fun isToolGloballyVisible(toolId: String): Boolean {
        if (!mcpEnabled) {
            return false
        }
        return toolVisibility.getOrDefault(toolId, true)
    }

    /**
     * Set the global visibility of a tool
     *
     * @param toolId The tool ID
     * @param visible true to expose to MCP server, false to hide completely
     */
    fun setToolGloballyVisible(toolId: String, visible: Boolean) {
        toolVisibility[toolId] = visible
    }

    override fun getState(): McpApplicationSettings = this

    override fun loadState(state: McpApplicationSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        @JvmStatic
        fun getInstance(): McpApplicationSettings =
            ApplicationManager.getApplication().getService(McpApplicationSettings::class.java)
    }
}
