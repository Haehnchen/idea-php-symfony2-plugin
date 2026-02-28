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
import fr.adrienbrault.idea.symfony2plugin.mcp.TwigComponentCollector
import kotlinx.coroutines.currentCoroutineContext

/**
 * MCP toolset for Symfony UX Twig components.
 */
class TwigComponentMcpToolset : McpToolset {

    @McpTool
    @McpDescription("""
        Lists Symfony UX Twig components and composition metadata.

        Supports partial component-name search (case-insensitive).

        Returns CSV format with columns:
        component_name,template_relative_path,component_tag,twig_component_syntax,component_print_block_syntax,twig_tag_composition_syntax,props,template_blocks

        - component_name: canonical component name (e.g. Alert, Admin:Card)
        - template_relative_path: component template path relative to project root
        - component_tag: HTML syntax (e.g. <twig:Alert></twig:Alert>)
        - twig_component_syntax: function syntax (e.g. {{ component('Alert') }})
        - component_print_block_syntax: block print calls for available blocks (e.g. {{ block('footer') }})
        - twig_tag_composition_syntax: Twig tag composition snippet ({% component 'Alert' %}...{% endcomponent %})
        - props: component props exposed by PHP class/template props tag
        - template_blocks: block names provided by component template
    """)
    suspend fun list_twig_components(
        @McpDescription("Optional partial component name filter (case-insensitive)")
        search: String? = null
    ): String {
        val project = currentCoroutineContext().project

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            mcpFail("Symfony plugin is not enabled for this project.")
        }

        McpUtil.checkToolEnabled(project, "list_twig_components")

        return readAction {
            TwigComponentCollector(project).collect(search)
        }
    }
}
