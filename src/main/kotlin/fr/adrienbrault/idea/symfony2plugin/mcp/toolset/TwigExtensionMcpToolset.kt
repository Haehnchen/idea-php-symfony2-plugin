@file:Suppress("FunctionName", "unused")

package fr.adrienbrault.idea.symfony2plugin.mcp.toolset

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.mcpserver.project
import com.intellij.openapi.application.readAction
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.mcp.McpUtil
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.TwigExtensionCollector
import kotlinx.coroutines.currentCoroutineContext

/**
 * MCP toolset for Twig extensions.
 * Provides unified access to all Twig extensions including filters, functions, tests, and tags.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigExtensionMcpToolset : McpToolset {

    @McpTool
    @McpDescription("""
        Lists available Twig template extensions for code generation and template assistance.

        Use this to discover:
        - Filters for value transformation ({{ value|filter }})
        - Functions for template logic ({{ function() }})
        - Tests for conditionals ({% if var is test %})
        - Tags for control structures ({% tag %})

        Supports filtering by name (partial match) and type. Use to generate accurate Twig code,
        validate template syntax, or suggest available extensions to developers.

        Returns CSV: extension_type,name,className,methodName,parameters
        Example: filter,upper,\Twig\Extension\CoreExtension,upper,"value,encoding"
    """)
    suspend fun list_twig_extensions(
        @McpDescription("Partial name search (case-insensitive). Examples: 'date', 'url', 'format'")
        search: String? = null,

        @McpDescription("Include filters ({{ value|filter }}). Default: true")
        includeFilters: Boolean = true,

        @McpDescription("Include functions ({{ func() }}). Default: true")
        includeFunctions: Boolean = true,

        @McpDescription("Include tests ({% if value is test %}). Default: true")
        includeTests: Boolean = true,

        @McpDescription("Include tags ({% tag %}). Default: true")
        includeTags: Boolean = true
    ): String {
        val project = currentCoroutineContext().project

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            mcpFail("Symfony plugin is not enabled for this project.")
        }

        McpUtil.checkToolEnabled(project, "list_twig_extensions")

        return readAction {
            TwigExtensionCollector(project).collect(search, includeFilters, includeFunctions, includeTests, includeTags)
        }
    }
}
