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
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyRoutePathCollector
import kotlinx.coroutines.currentCoroutineContext

/**
 * MCP toolset for matching URL paths to Symfony routes.
 * Provides reverse pattern matching to find routes that could handle a given URL.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class RoutePathMcpToolset : McpToolset {

    @McpTool
    @McpDescription("""
        Matches a plain request URL to Symfony routes using reverse pattern matching.

        This tool finds routes that could handle the given URL path by matching against
        route patterns with placeholders. It supports partial matching and can identify
        routes even when the URL contains values for placeholders.

        Returns CSV format with columns: name,controller,path,filePath
        - name: Route name
        - controller: Controller class::method
        - path: URL path pattern
        - filePath: Relative path to controller file

        Example output:
        name,controller,path,filePath
        api_user_show,App\Controller\Api\UserController::show,/api/users/{id},src/Controller/Api/UserController.php
        api_user_list,App\Controller\Api\UserController::list,/api/users,src/Controller/Api/UserController.php
    """)
    suspend fun match_symfony_url_to_route(
        @McpDescription("The request URL path to match (e.g., '/api/users/123', '/blog/my-post')")
        urlPath: String
    ): String {
        val project = currentCoroutineContext().project

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            mcpFail("Symfony plugin is not enabled for this project.")
        }

        McpUtil.checkToolEnabled(project, "match_symfony_url_to_route")

        return readAction {
            SymfonyRoutePathCollector(project).collect(urlPath)
        }
    }
}
