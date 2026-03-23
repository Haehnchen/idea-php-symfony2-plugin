package fr.adrienbrault.idea.symfony2plugin.mcp.collector

import com.intellij.mcpserver.mcpFail
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil
import com.intellij.openapi.project.Project
import fr.adrienbrault.idea.symfony2plugin.profiler.collector.DefaultDataCollectorInterface
import fr.adrienbrault.idea.symfony2plugin.profiler.factory.ProfilerFactoryUtil
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequestInterface
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil

class SymfonyProfilerRequestsCollector(private val project: Project) {
    fun collect(
        url: String? = null,
        hash: String? = null,
        controller: String? = null,
        route: String? = null,
        limit: Int = 25
    ): String {
        val profilerIndex = ProfilerFactoryUtil.createIndex(project)
            ?: mcpFail("No profiler index available. Make sure the Symfony profiler is enabled and accessible.")

        val requests = profilerIndex.requests

        if (requests.isEmpty()) {
            mcpFail("No profiler requests found. Make sure the Symfony profiler is enabled and has recorded requests.")
        }

        return formatRequests(requests, url, hash, controller, route, limit)
    }

    internal fun formatRequests(
        requests: Iterable<ProfilerRequestInterface>,
        url: String? = null,
        hash: String? = null,
        controller: String? = null,
        route: String? = null,
        limit: Int = 25
    ): String {
        val effectiveLimit = if (limit > 0) limit else 25
        val filteredRequests = requests.filter { request ->
            val collectorData = request.getCollector(DefaultDataCollectorInterface::class.java)

            val controllerValue = collectorData?.controller ?: ""
            val routeValue = collectorData?.route ?: ""

            if (url != null && !request.url.contains(url, ignoreCase = true)) return@filter false
            if (hash != null && !request.hash.contains(hash, ignoreCase = true)) return@filter false
            if (controller != null && !controllerValue.contains(controller, ignoreCase = true)) return@filter false
            if (route != null && !routeValue.contains(route, ignoreCase = true)) return@filter false

            true
        }.take(effectiveLimit)

        val templatesByController = TwigUtil.findTemplatesByControllers(
            project,
            filteredRequests
                .mapNotNull { it.getCollector(DefaultDataCollectorInterface::class.java)?.controller }
                .filter { it.isNotBlank() }
                .toSet()
        )

        return buildString {
            appendLine("hash,method,url,statusCode,profilerUrl,controller,route,entryView,renderTemplate,renderedTemplates,formTypes")
            for (request in filteredRequests) {
                val collectorData = request.getCollector(DefaultDataCollectorInterface::class.java)

                val controllerValue = collectorData?.controller ?: ""
                val routeValue = collectorData?.route ?: ""
                val entryView = collectorData?.template ?: ""
                val renderTemplate = templatesByController[normalizeControllerScope(controllerValue)]
                    ?.toList()
                    ?.sorted()
                    ?.joinToString(";")
                    ?: ""
                val renderedTemplates = collectorData?.renderedTemplates
                    ?.take(3)
                    ?.joinToString(";")
                    ?: ""
                val formTypes = collectorData?.formTypes?.joinToString("|") ?: ""

                appendLine(
                    "${McpCsvUtil.escape(request.hash)}," +
                        "${McpCsvUtil.escape(request.method ?: "")}," +
                        "${McpCsvUtil.escape(request.url)}," +
                        "${request.statusCode}," +
                        "${McpCsvUtil.escape(request.profilerUrl)}," +
                        "${McpCsvUtil.escape(controllerValue)}," +
                        "${McpCsvUtil.escape(routeValue)}," +
                        "${McpCsvUtil.escape(entryView)}," +
                        "${McpCsvUtil.escape(renderTemplate)}," +
                        "${McpCsvUtil.escape(renderedTemplates)}," +
                        McpCsvUtil.escape(formTypes)
                )
            }
        }
    }

    private fun normalizeControllerScope(controller: String): String {
        return controller.trimStart('\\').replace("::", ".")
    }
}
