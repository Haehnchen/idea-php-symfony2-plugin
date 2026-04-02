package fr.adrienbrault.idea.symfony2plugin.mcp

import com.intellij.mcpserver.McpTool
import com.intellij.mcpserver.McpToolsProvider
import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.impl.util.asTools
import fr.adrienbrault.idea.symfony2plugin.mcp.toolset.*

/**
 * MCP Tools Provider for Symfony Plugin.
 * Provides tools to the MCP server, filtering based on application-level settings.
 * Tools disabled in application settings are completely hidden from the MCP server.
 *
 * Note: The asTools() extension function uses reflection to extract @McpTool annotated
 * methods from each toolset class. The tool name is derived from the method name.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class SymfonyMcpToolsProvider : McpToolsProvider {

    override fun getTools(): List<McpTool> {
        val settings = McpApplicationSettings.getInstance()

        // If MCP is globally disabled, return no tools
        if (!settings.mcpEnabled) {
            return emptyList()
        }

        // All Symfony MCP toolsets - each toolset contains @McpTool annotated methods
        val allToolsets: List<McpToolset> = listOf(
            RouteMcpToolset(),
            CommandMcpToolset(),
            DoctrineEntityMcpToolset(),
            DoctrineEntityFieldsMcpToolset(),
            TwigExtensionMcpToolset(),
            ProfilerRequestsMcpToolset(),
            FormTypeMcpToolset(),
            FormTypeOptionsMcpToolset(),
            ServiceMcpToolset(),
            ServiceDefinitionMcpToolset(),
            TwigTemplateUsageMcpToolset(),
            TwigComponentMcpToolset(),
            TwigTemplateVariablesMcpToolset()
        )

        val tools = mutableListOf<McpTool>()

        for (toolset in allToolsets) {
            try {
                val toolsetTools = toolset.asTools()

                // Filter tools based on application-level visibility settings
                for (tool in toolsetTools) {
                    val toolName = tool.descriptor.name
                    if (settings.isToolGloballyVisible(toolName)) {
                        tools.add(tool)
                    }
                }
            } catch (e: Exception) {
                // Log error but continue with other toolsets
                println("[Symfony MCP] Error loading tools from ${toolset.javaClass.simpleName}: ${e.message}")
            }
        }

        return tools
    }
}
