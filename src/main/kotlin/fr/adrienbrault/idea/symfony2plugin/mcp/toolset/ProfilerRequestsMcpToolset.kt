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
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyProfilerRequestsCollector
import kotlinx.coroutines.currentCoroutineContext

/**
 * MCP toolset for Symfony profiler requests.
 * Provides access to recent profiler requests.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class ProfilerRequestsMcpToolset : McpToolset {

    @McpTool
    @McpDescription("""
        Lists recent Symfony profiler requests as CSV for tracing request handling, routes, controller and rendered views.

        - hash: Profiler token/hash
        - method: HTTP method (GET, POST, etc.)
        - url: Request URL
        - statusCode: HTTP status code
        - profilerUrl: URL to the profiler page
        - controller: Controller handling the request
        - route: Matched route name
        - entryView: Symfony profiler "Entry View"
        - renderTemplate: Templates indexed from render*()/@Template/#[Template], joined by semicolon
        - formTypes: Form types used (pipe-separated if multiple)

        Example output:
        hash,method,url,statusCode,profilerUrl,controller,route,entryView,renderTemplate,formTypes
        18e6b8,GET,http://127.0.0.1:8000/user/1,200,_profiler/18e6b8,App\Controller\UserController::show,user_show,user/show.html.twig,user/show.html.twig;user/_card.html.twig,
    """)
    suspend fun list_profiler_requests(
        @McpDescription("Optional URL partial match, case-insensitive. Example: '/admin' or '127.0.0.1:8000'")
        url: String? = null,

        @McpDescription("Optional profiler token partial match, case-insensitive. Example: '18e6b8'")
        hash: String? = null,

        @McpDescription("Optional controller partial match, case-insensitive. Example: 'UserController' or '::show'")
        controller: String? = null,

        @McpDescription("Optional route partial match, case-insensitive. Example: 'user_show'")
        route: String? = null,

        @McpDescription("Optional max rows. Default: 30. Example: 5")
        limit: Int = 30
    ): String {
        val project = currentCoroutineContext().project

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            mcpFail("Symfony plugin is not enabled for this project.")
        }

        McpUtil.checkToolEnabled(project, "list_profiler_requests")

        return readAction {
            SymfonyProfilerRequestsCollector(project).collect(url, hash, controller, route, limit)
        }
    }
}
