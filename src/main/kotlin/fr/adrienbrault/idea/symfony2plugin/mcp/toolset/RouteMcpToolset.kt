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
        Lists Symfony routes with their controller mappings, file paths, and Twig template usages.

        Use this tool to find routes and their associated controllers in the Symfony project.
        This is essential for understanding the relationship between URL paths, route names,
        controller classes, their physical file locations, and the Twig templates they render.

        Key features:
        - Search routes by name or controller
        - Returns controller class and method for each route
        - Provides file path to controller implementation
        - Lists Twig templates rendered/used by each controller method (render(), renderView(), @Template)
        - Supports partial matching for flexible searches

        Returns CSV format with columns: name,controller,path,filePath,templates
        - name: Route name
        - controller: Full controller class::method reference
        - path: URL path pattern (may contain parameters like {id})
        - filePath: Relative path to controller file from project root
        - templates: Semicolon-separated list of Twig templates used by this controller action.
                     These are templates referenced via render(), renderView(), renderResponse().

        Example output:
        name,controller,path,filePath,templates
        app_home,App\Controller\HomeController::index,/,src/Controller/HomeController.php,home/index.html.twig
        app_user_list,App\Controller\UserController::list,/users,src/Controller/UserController.php,user/list.html.twig;user/_pagination.html.twig
        app_api_users,App\Controller\ApiController::users,/api/users,src/Controller/ApiController.php,
    """)
    suspend fun list_symfony_routes_controllers(
        @McpDescription("Optional: Filter by route name (supports partial matching, case-insensitive)")
        routeName: String? = null,

        @McpDescription("Optional: Filter by controller class or method (supports partial matching, case-insensitive)")
        controller: String? = null
    ): String {
        val project = currentCoroutineContext().project

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            mcpFail("Symfony plugin is not enabled for this project.")
        }

        McpUtil.checkToolEnabled(project, "list_symfony_routes_controllers")

        return readAction {
            SymfonyRouteCollector(project).collect(routeName, controller)
        }
    }
}
