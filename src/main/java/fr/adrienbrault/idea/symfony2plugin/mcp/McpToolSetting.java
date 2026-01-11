package fr.adrienbrault.idea.symfony2plugin.mcp;

import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the configuration for an individual MCP tool
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@Tag("mcp_tool")
public class McpToolSetting {

    private String toolId;
    private String toolName;
    private String description;
    private boolean enabled = true;

    public McpToolSetting() {
    }

    public McpToolSetting(@NotNull String toolId, @NotNull String toolName, @NotNull String description) {
        this.toolId = toolId;
        this.toolName = toolName;
        this.description = description;
        this.enabled = true;
    }

    public McpToolSetting(@NotNull String toolId, @NotNull String toolName, @NotNull String description, boolean enabled) {
        this.toolId = toolId;
        this.toolName = toolName;
        this.description = description;
        this.enabled = enabled;
    }

    @Attribute("toolId")
    @NotNull
    public String getToolId() {
        return toolId != null ? toolId : "";
    }

    public void setToolId(@NotNull String toolId) {
        this.toolId = toolId;
    }

    @Attribute("toolName")
    @NotNull
    public String getToolName() {
        return toolName != null ? toolName : "";
    }

    public void setToolName(@NotNull String toolName) {
        this.toolName = toolName;
    }

    @Attribute("description")
    @NotNull
    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(@NotNull String description) {
        this.description = description;
    }

    @Attribute("enabled")
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
