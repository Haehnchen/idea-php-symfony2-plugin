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
        Lists the last 10 Symfony profiler requests as CSV.

        Returns CSV format with columns: hash,method,url,statusCode,profilerUrl,controller,route,template,formTypes
        - hash: Profiler token/hash
        - method: HTTP method (GET, POST, etc.)
        - url: Request URL
        - statusCode: HTTP status code
        - profilerUrl: URL to the profiler page
        - controller: Controller handling the request
        - route: Matched route name
        - template: Main template rendered
        - formTypes: Form types used (pipe-separated if multiple)

        Example output:
        hash,method,url,statusCode,profilerUrl,controller,route,template,formTypes
        18e6b8,GET,http://127.0.0.1:8000/user/1,200,_profiler/18e6b8,App\Controller\UserController::show,user_show,user/show.html.twig,
    """)
    suspend fun list_profiler_requests(
        @McpDescription("Optional: Filter by URL (supports partial matching, case-insensitive)")
        url: String? = null,

        @McpDescription("Optional: Filter by profiler token/hash (supports partial matching, case-insensitive)")
        hash: String? = null,

        @McpDescription("Optional: Filter by controller class or method (supports partial matching, case-insensitive)")
        controller: String? = null,

        @McpDescription("Optional: Filter by route name (supports partial matching, case-insensitive)")
        route: String? = null
    ): String {
        val project = currentCoroutineContext().project

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            mcpFail("Symfony plugin is not enabled for this project.")
        }

        McpUtil.checkToolEnabled(project, "list_profiler_requests")

        return readAction {
            SymfonyProfilerRequestsCollector(project).collect(url, hash, controller, route)
        }
    }
}
