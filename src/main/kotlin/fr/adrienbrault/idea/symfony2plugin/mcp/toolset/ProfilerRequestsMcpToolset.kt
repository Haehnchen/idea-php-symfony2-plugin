@file:Suppress("FunctionName", "unused")

package fr.adrienbrault.idea.symfony2plugin.mcp.toolset

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.mcpserver.project
import com.intellij.openapi.application.readAction
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
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
        Lists recent Symfony requests from the Profiler as CSV for tracing request handling, controller resolution, and rendered Twig views.

        - hash: Profiler token/hash
        - method: HTTP method (GET, POST, etc.)
        - url: Request URL
        - statusCode: HTTP status code
        - profilerUrl: URL to the profiler page
        - controller: Controller handling the request
        - route: Matched route name
        - entryView: Symfony profiler "Entry View"
        - renderTemplate: Templates indexed from render*()/@Template/#[Template], joined by semicolon
        - renderedTemplates: First 3 Twig templates from the profiler "Rendered Templates" panel, in profiler order, joined by semicolon
        - formTypes: Form types used (pipe-separated if multiple)
    """)
    suspend fun list_profiler_requests(
        @McpDescription("Optional URL partial match, case-insensitive. Example: '/admin'")
        url: String? = null,

        @McpDescription("Optional profiler token partial match, case-insensitive. Example: '18e6b8'")
        hash: String? = null,

        @McpDescription("Optional controller partial match, case-insensitive. Example: 'UserController' or '::show'")
        controller: String? = null,

        @McpDescription("Optional route partial match, case-insensitive. Example: 'user_show'")
        route: String? = null,

        @McpDescription("Optional max rows. Default: 25.")
        limit: Int = 25
    ): String {
        val project = currentCoroutineContext().project

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            mcpFail("Symfony plugin is not enabled for this project.")
        }

        return readAction {
            SymfonyProfilerRequestsCollector(project).collect(url, hash, controller, route, limit)
        }
    }
}
