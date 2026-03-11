package fr.adrienbrault.idea.symfony2plugin.mcp;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
    storages = {
        @Storage("symfony-mcp.xml")
    }
)
public class McpSettings implements PersistentStateComponent<McpSettings> {

    /**
     * Configuration for individual MCP tools (per-project)
     */
    @Nullable
    public List<McpToolSetting> mcpToolSettings = new ArrayList<>();

    public static McpSettings getInstance(Project project) {
        return project.getService(McpSettings.class);
    }

    @Nullable
    @Override
    public McpSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull McpSettings settings) {
        XmlSerializerUtil.copyBean(settings, this);
    }
}
