package fr.adrienbrault.idea.symfony2plugin.mcp.collector

import com.intellij.openapi.project.Project
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil
import fr.adrienbrault.idea.symfony2plugin.mcp.McpPathUtil
import com.jetbrains.php.PhpIndex
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import org.apache.commons.lang3.StringUtils

class SymfonyRouteCollector(private val project: Project) {
    fun collect(routeName: String? = null, controller: String? = null): String {
        val allRoutes = RouteHelper.getAllRoutes(project)
        val phpIndex = PhpIndex.getInstance(project)

        val filteredRoutes = allRoutes.filter { (name, route) ->
            val matchesName = routeName == null || name.contains(routeName, ignoreCase = true)
            val matchesController = controller == null || route.controller?.contains(controller, ignoreCase = true) == true
            matchesName && matchesController
        }

        val controllerFqns = filteredRoutes
            .mapNotNull { it.value.controller }
            .filter { it.contains("::") }
            .toSet()

        val templatesByController = TwigUtil.findTemplatesByControllers(project, controllerFqns)

        val csv = StringBuilder("name,controller,path,filePath,templates\\n")

        filteredRoutes.forEach { (_, route) ->
            val controllerClass = route.controller?.let { ctrl ->
                if (ctrl.contains("::")) ctrl.substringBefore("::") else ctrl
            }

            val filePath = controllerClass?.let { className ->
                val fqn = if (className.startsWith("\\")) className else "\\$className"
                phpIndex.getClassesByFQN(fqn).firstOrNull()
                    ?.containingFile
                    ?.virtualFile
                    ?.let { McpPathUtil.getRelativeProjectPath(project, it) }
            } ?: ""

            val normalizedController = route.controller
                ?.replace("::", ".")
                ?.let { StringUtils.stripStart(it, "\\") }

            val templates = normalizedController?.let { ctrl ->
                templatesByController[ctrl]?.joinToString(";") ?: ""
            } ?: ""

            csv.append("${McpCsvUtil.escape(route.name)},${McpCsvUtil.escape(route.controller ?: "")},${McpCsvUtil.escape(route.path ?: "")},${McpCsvUtil.escape(filePath)},${McpCsvUtil.escape(templates)}\\n")
        }

        return csv.toString()
    }
}
