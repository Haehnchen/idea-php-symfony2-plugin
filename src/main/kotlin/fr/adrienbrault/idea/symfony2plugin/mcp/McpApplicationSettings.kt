package fr.adrienbrault.idea.symfony2plugin.mcp

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Application-level settings for MCP (Model Context Protocol) tools.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@State(
    name = "SymfonyMcpApplicationSettings",
    storages = [Storage("symfony-mcp-application.xml")]
)
class McpApplicationSettings : PersistentStateComponent<McpApplicationSettings> {

    /** Master enable/disable for all MCP tools globally. */
    @JvmField
    var mcpEnabled: Boolean = true

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
