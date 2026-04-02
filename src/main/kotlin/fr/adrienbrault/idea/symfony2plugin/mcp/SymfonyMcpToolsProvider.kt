package fr.adrienbrault.idea.symfony2plugin.mcp

import com.intellij.mcpserver.McpTool
import com.intellij.mcpserver.McpToolsProvider
import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.impl.util.asTools
import fr.adrienbrault.idea.symfony2plugin.mcp.toolset.*

/**
 * MCP Tools Provider for Symfony Plugin.
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

        return allToolsets.flatMap { toolset ->
            try {
                toolset.asTools()
            } catch (e: Exception) {
                println("[Symfony MCP] Error loading tools from ${toolset.javaClass.simpleName}: ${e.message}")
                emptyList()
            }
        }
    }
}
