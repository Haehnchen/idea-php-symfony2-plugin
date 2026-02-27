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
import fr.adrienbrault.idea.symfony2plugin.mcp.TwigTemplateUsageCollector
import kotlinx.coroutines.currentCoroutineContext

/**
 * MCP toolset for Twig template usage analysis.
 * Given a (partial) template name, returns all locations where it is used across the project.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigTemplateUsageMcpToolset : McpToolset {

    @McpTool
    @McpDescription("""
        Lists usages of Twig templates. Accepts a partial template name or a project-relative file path (e.g. "templates/home/index.html.twig" → resolves to relative template "home/index.html.twig").

        CSV columns (values semicolon-separated): template,controller,twig_include,twig_embed,twig_extends,twig_import,twig_use,twig_form_theme,twig_component
        - template: logical template name (e.g. "home/index.html.twig")
        - controller: PHP methods via render()/renderView()/@Template/#[Template]
        - twig_include: {% include %} / {{ include() }}
        - twig_embed: {% embed %}
        - twig_extends: {% extends %}
        - twig_import: {% import %} / {% from ... import %}
        - twig_use: {% use %}
        - twig_form_theme: {% form_theme %}
        - twig_component: {{ component('X') }} / {% component X %} / <twig:X>

        Example:
        template,controller,twig_include,twig_embed,twig_extends,twig_import,twig_use,twig_form_theme,twig_component
        partials/nav.html.twig,App\Controller\BaseController::index,templates/layouts/base.html.twig,,,,,,,
        layouts/base.html.twig,,,,,templates/pages/home.html.twig;templates/pages/about.html.twig,,,,
    """)
    suspend fun list_twig_template_usages(
        @McpDescription("Partial template name or project-relative file path")
        template: String
    ): String {
        val project = currentCoroutineContext().project

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            mcpFail("Symfony plugin is not enabled for this project.")
        }

        McpUtil.checkToolEnabled(project, "list_twig_template_usages")

        return readAction {
            TwigTemplateUsageCollector(project).collect(template)
        }
    }
}
