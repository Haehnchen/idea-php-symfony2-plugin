package fr.adrienbrault.idea.symfony2plugin.mcp

import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag

/**
 * Represents the configuration for an individual MCP tool
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@Tag("mcp_tool")
class McpToolSetting {

    private var toolId: String? = null
    private var toolName: String? = null
    private var description: String? = null
    private var enabled: Boolean = true

    constructor()

    constructor(toolId: String, toolName: String, description: String) {
        this.toolId = toolId
        this.toolName = toolName
        this.description = description
        this.enabled = true
    }

    constructor(toolId: String, toolName: String, description: String, enabled: Boolean) {
        this.toolId = toolId
        this.toolName = toolName
        this.description = description
        this.enabled = enabled
    }

    @Attribute("toolId")
    fun getToolId(): String = toolId ?: ""

    fun setToolId(toolId: String) {
        this.toolId = toolId
    }

    @Attribute("toolName")
    fun getToolName(): String = toolName ?: ""

    fun setToolName(toolName: String) {
        this.toolName = toolName
    }

    @Attribute("description")
    fun getDescription(): String = description ?: ""

    fun setDescription(description: String) {
        this.description = description
    }

    @Attribute("enabled")
    fun isEnabled(): Boolean = enabled

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
}
