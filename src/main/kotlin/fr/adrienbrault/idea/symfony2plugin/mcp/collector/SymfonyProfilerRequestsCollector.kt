package fr.adrienbrault.idea.symfony2plugin.mcp.collector

import com.intellij.mcpserver.mcpFail
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil
import com.intellij.openapi.project.Project
import fr.adrienbrault.idea.symfony2plugin.profiler.collector.DefaultDataCollectorInterface
import fr.adrienbrault.idea.symfony2plugin.profiler.factory.ProfilerFactoryUtil
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequestInterface

class SymfonyProfilerRequestsCollector(private val project: Project) {
    fun collect(
        url: String? = null,
        hash: String? = null,
        controller: String? = null,
        route: String? = null
    ): String {
        val profilerIndex = ProfilerFactoryUtil.createIndex(project)
            ?: mcpFail("No profiler index available. Make sure the Symfony profiler is enabled and accessible.")

        val requests = profilerIndex.requests

        if (requests.isEmpty()) {
            mcpFail("No profiler requests found. Make sure the Symfony profiler is enabled and has recorded requests.")
        }

        return formatRequests(requests, url, hash, controller, route)
    }

    internal fun formatRequests(
        requests: Iterable<ProfilerRequestInterface>,
        url: String? = null,
        hash: String? = null,
        controller: String? = null,
        route: String? = null
    ): String = buildString {
        appendLine("hash,method,url,statusCode,profilerUrl,controller,route,template,formTypes")
        var count = 0
        for (request in requests) {
            if (count >= 10) break

            val collectorData = request.getCollector(DefaultDataCollectorInterface::class.java)

            val controllerValue = collectorData?.controller ?: ""
            val routeValue = collectorData?.route ?: ""
            val template = collectorData?.template ?: ""
            val formTypes = collectorData?.formTypes?.joinToString("|") ?: ""

            if (url != null && !request.url.contains(url, ignoreCase = true)) continue
            if (hash != null && !request.hash.contains(hash, ignoreCase = true)) continue
            if (controller != null && !controllerValue.contains(controller, ignoreCase = true)) continue
            if (route != null && !routeValue.contains(route, ignoreCase = true)) continue

            appendLine(
                "${McpCsvUtil.escape(request.hash)}," +
                    "${McpCsvUtil.escape(request.method ?: "")}," +
                    "${McpCsvUtil.escape(request.url)}," +
                    "${request.statusCode}," +
                    "${McpCsvUtil.escape(request.profilerUrl)}," +
                    "${McpCsvUtil.escape(controllerValue)}," +
                    "${McpCsvUtil.escape(routeValue)}," +
                    "${McpCsvUtil.escape(template)}," +
                    McpCsvUtil.escape(formTypes)
            )

            count++
        }
    }
}
