@file:Suppress("FunctionName", "unused")

package fr.adrienbrault.idea.symfony2plugin.mcp.toolset

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.project
import com.intellij.openapi.application.readAction
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.mcp.McpUtil
import fr.adrienbrault.idea.symfony2plugin.profiler.collector.DefaultDataCollectorInterface
import fr.adrienbrault.idea.symfony2plugin.profiler.factory.ProfilerFactoryUtil
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

        Parameters (all optional, partial match, case-insensitive):
        - url: Filter by URL (e.g., "/user" matches "/user/1", "/api/user")
        - hash: Filter by profiler token/hash
        - controller: Filter by controller name (e.g., "User" matches "UserController")
        - route: Filter by route name (e.g., "user" matches "user_show", "api_user_list")

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
            throw IllegalStateException("Symfony plugin is not enabled for this project.")
        }

        McpUtil.checkToolEnabled(project, "list_profiler_requests")

        return readAction {
            val profilerIndex = ProfilerFactoryUtil.createIndex(project)
                ?: throw IllegalStateException("No profiler index available. Make sure the Symfony profiler is enabled and accessible.")

            val requests = profilerIndex.requests

            if (requests.isEmpty()) {
                throw IllegalStateException("No profiler requests found. Make sure the Symfony profiler is enabled and has recorded requests.")
            }

            val csv = StringBuilder("hash,method,url,statusCode,profilerUrl,controller,route,template,formTypes\n")

            var count = 0
            for (request in requests) {
                if (count >= 10) break

                val collector = request.getCollector(DefaultDataCollectorInterface::class.java)

                val controllerValue = collector?.controller ?: ""
                val routeValue = collector?.route ?: ""
                val template = collector?.template ?: ""
                val formTypes = collector?.formTypes?.joinToString("|") ?: ""

                // Apply filters (all optional, partial match, case-insensitive)
                if (url != null && !request.url.contains(url, ignoreCase = true)) continue
                if (hash != null && !request.hash.contains(hash, ignoreCase = true)) continue
                if (controller != null && !controllerValue.contains(controller, ignoreCase = true)) continue
                if (route != null && !routeValue.contains(route, ignoreCase = true)) continue

                csv.append("${escapeCsv(request.hash)},")
                csv.append("${escapeCsv(request.method ?: "")},")
                csv.append("${escapeCsv(request.url)},")
                csv.append("${request.statusCode},")
                csv.append("${escapeCsv(request.profilerUrl)},")
                csv.append("${escapeCsv(controllerValue)},")
                csv.append("${escapeCsv(routeValue)},")
                csv.append("${escapeCsv(template)},")
                csv.append("${escapeCsv(formTypes)}\n")

                count++
            }

            csv.toString()
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
