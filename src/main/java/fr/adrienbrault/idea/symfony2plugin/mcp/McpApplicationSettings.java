package fr.adrienbrault.idea.symfony2plugin.mcp;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Application-level settings for MCP (Model Context Protocol) tools.
 * Controls which tools are globally visible to the MCP server.
 * Tools disabled here are completely hidden from the MCP server.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@State(
    name = "SymfonyMcpApplicationSettings",
    storages = {
        @Storage("symfony-mcp-application.xml")
    }
)
public class McpApplicationSettings implements PersistentStateComponent<McpApplicationSettings> {

    /**
     * Master enable/disable for all MCP tools globally.
     * When disabled, no tools are exposed to the MCP server.
     */
    public boolean mcpEnabled = true;

    /**
     * Map of toolId -> enabled (globally visible to MCP server)
     * Tools not in this map are considered enabled by default
     */
    @NotNull
    public Map<String, Boolean> toolVisibility = new HashMap<>();

    public static McpApplicationSettings getInstance() {
        return ApplicationManager.getApplication().getService(McpApplicationSettings.class);
    }

    /**
     * Check if MCP tools are globally enabled
     *
     * @return true if MCP tools should be exposed to the MCP server
     */
    public boolean isMcpEnabled() {
        return mcpEnabled;
    }

    /**
     * Check if a tool is globally visible to the MCP server.
     * Returns false if MCP is globally disabled, or if the specific tool is disabled.
     *
     * @param toolId The tool ID to check
     * @return true if the tool should be exposed to MCP server
     */
    public boolean isToolGloballyVisible(@NotNull String toolId) {
        // If MCP is globally disabled, no tools are visible
        if (!mcpEnabled) {
            return false;
        }
        // Default to enabled if not explicitly set
        return toolVisibility.getOrDefault(toolId, true);
    }

    /**
     * Set the global visibility of a tool
     *
     * @param toolId The tool ID
     * @param visible true to expose to MCP server, false to hide completely
     */
    public void setToolGloballyVisible(@NotNull String toolId, boolean visible) {
        toolVisibility.put(toolId, visible);
    }

    @Nullable
    @Override
    public McpApplicationSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull McpApplicationSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
