package fr.adrienbrault.idea.symfony2plugin.mcp

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Per-project settings for MCP (Model Context Protocol) tools configuration.
 * This is stored separately from main Symfony plugin settings.
 *
 * Note: Global MCP enable/disable is in McpApplicationSettings (application-level).
 * This class only contains per-project tool settings.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@State(
    name = "SymfonyMcpSettings",
    storages = [Storage("symfony-mcp.xml")]
)
class McpSettings : PersistentStateComponent<McpSettings> {

    /**
     * Configuration for individual MCP tools (per-project)
     */
    @JvmField
    var mcpToolSettings: MutableList<McpToolSetting>? = ArrayList()

    override fun getState(): McpSettings = this

    override fun loadState(settings: McpSettings) {
        XmlSerializerUtil.copyBean(settings, this)
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): McpSettings = project.getService(McpSettings::class.java)
    }
}
