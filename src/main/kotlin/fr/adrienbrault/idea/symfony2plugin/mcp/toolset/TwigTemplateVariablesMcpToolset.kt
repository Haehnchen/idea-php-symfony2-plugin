@file:Suppress("FunctionName", "unused")

package fr.adrienbrault.idea.symfony2plugin.mcp.toolset

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.mcpserver.project
import com.intellij.openapi.application.readAction
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.TwigTemplateVariablesCollector
import kotlinx.coroutines.currentCoroutineContext

/**
 * MCP toolset for listing Twig template variables with their PHP types and properties.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigTemplateVariablesMcpToolset : McpToolset {

    @McpTool
    @McpDescription("""
        Lists all variables available in a Twig template with their PHP types and first-level accessible properties.
        Accepts a template name or a project-relative file path (e.g. "templates/home/index.html.twig" → resolves to "home/index.html.twig").

        CSV columns:
        - variable: Twig variable name
        - type: PHP FQN(s) joined with "|" (may include "[]" suffix for arrays)
        - properties: comma-separated first-level Twig-accessible names (get/is/has shortcut methods + public fields)

        Example:
        variable,type,properties
        user,\App\Entity\User,"id,email,name,roles,createdAt"
        app,\Symfony\Bridge\Twig\AppVariable,"user,request,session,environment,debug,token,flashes"
        products,\App\Entity\Product[],"id,title,price,category,isActive"
    """)
    suspend fun list_twig_template_variables(
        @McpDescription("Template name (e.g. 'home/index.html.twig') or project-relative path (e.g. 'templates/home/index.html.twig')")
        template: String
    ): String {
        val project = currentCoroutineContext().project

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            mcpFail("Symfony plugin is not enabled for this project.")
        }

        return readAction {
            TwigTemplateVariablesCollector(project).collect(template)
        }
    }
}
