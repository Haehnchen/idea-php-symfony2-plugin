@file:Suppress("FunctionName", "unused")

package fr.adrienbrault.idea.symfony2plugin.mcp.toolset

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.mcpserver.project
import com.intellij.openapi.application.readAction
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyRouteCollector
import kotlinx.coroutines.currentCoroutineContext

/**
 * MCP toolset for Symfony routing information.
 * Provides access to routes configured in the Symfony project.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class RouteMcpToolset : McpToolset {

    @McpTool
    @McpDescription("""
        Lists Symfony routes with URL mappings, controller mappings, file paths, and Twig template usages.

        Use this tool to find routes by route name, controller, partial route path, or any partial request URL.

        Returns CSV format with columns: name,controller,path,filePath,lineNumber,templates
        - name: Route name
        - controller: Full controller class::method reference
        - path: URL path pattern (contains parameters {id})
        - filePath: Relative path to controller file from project root
        - lineNumber: Controller method line number in the file; empty if unresolved
        - templates: Semicolon-separated list of Twig templates used by this controller references via render*() or php attributes

        Example output:
        name,controller,path,filePath,lineNumber,templates
        app_user_list,App\Controller\UserController::list,/users,src/Controller/UserController.php,42,user/list.html.twig;user/_pagination.html.twig
    """)
    suspend fun list_symfony_routes_url_controllers(
        @McpDescription("Optional: Filter by route name (partial matching, case-insensitive)")
        routeName: String? = null,

        @McpDescription("Optional: Filter by controller class or method (partial matching, case-insensitive)")
        controller: String? = null,

        @McpDescription("Optional: Filter by route path (edit/{id}) or url with reverse placeholder matching (edit/12) (all partial matching)")
        urlPath: String? = null
    ): String {
        val project = currentCoroutineContext().project

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            mcpFail("Symfony plugin is not enabled for this project.")
        }

        return readAction {
            SymfonyRouteCollector(project).collect(routeName, controller, urlPath)
        }
    }
}
