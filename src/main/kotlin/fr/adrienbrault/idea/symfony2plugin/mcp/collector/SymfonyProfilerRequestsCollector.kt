package fr.adrienbrault.idea.symfony2plugin.mcp.collector

import com.intellij.mcpserver.mcpFail
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil
import fr.adrienbrault.idea.symfony2plugin.mcp.McpPathUtil
import com.intellij.openapi.project.Project
import fr.adrienbrault.idea.symfony2plugin.profiler.collector.DefaultDataCollectorInterface
import fr.adrienbrault.idea.symfony2plugin.profiler.factory.ProfilerFactoryUtil

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

        val csv = StringBuilder("hash,method,url,statusCode,profilerUrl,controller,route,template,formTypes\\n")

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

            csv.append("${McpCsvUtil.escape(request.hash)},")
            csv.append("${McpCsvUtil.escape(request.method ?: "")},")
            csv.append("${McpCsvUtil.escape(request.url)},")
            csv.append("${request.statusCode},")
            csv.append("${McpCsvUtil.escape(request.profilerUrl)},")
            csv.append("${McpCsvUtil.escape(controllerValue)},")
            csv.append("${McpCsvUtil.escape(routeValue)},")
            csv.append("${McpCsvUtil.escape(template)},")
            csv.append("${McpCsvUtil.escape(formTypes)}\\n")

            count++
        }

        return csv.toString()
    }
}
