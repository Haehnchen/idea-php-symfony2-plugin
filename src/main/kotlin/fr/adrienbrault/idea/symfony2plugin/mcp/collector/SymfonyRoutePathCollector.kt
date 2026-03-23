package fr.adrienbrault.idea.symfony2plugin.mcp.collector

import com.intellij.openapi.project.Project
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil
import fr.adrienbrault.idea.symfony2plugin.mcp.McpPathUtil
import com.jetbrains.php.PhpIndex
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper

class SymfonyRoutePathCollector(private val project: Project) {
    fun collect(urlPath: String): String = buildString {
        val matches = RouteHelper.getMethodsForPathWithPlaceholderMatchRoutes(project, urlPath)
        val phpIndex = PhpIndex.getInstance(project)
        appendLine("name,controller,path,filePath")

        matches.forEach { (route, _) ->
            val controllerClass = route.controller?.let { ctrl ->
                if ("::" in ctrl) ctrl.substringBefore("::") else ctrl
            }

            val filePath = controllerClass?.let { className ->
                val fqn = if (className.startsWith("\\")) className else "\\$className"
                phpIndex.getClassesByFQN(fqn).firstOrNull()
                    ?.containingFile
                    ?.virtualFile
                    ?.let { McpPathUtil.getRelativeProjectPath(project, it) }
            } ?: ""

            appendLine("${McpCsvUtil.escape(route.name)},${McpCsvUtil.escape(route.controller ?: "")},${McpCsvUtil.escape(route.path ?: "")},${McpCsvUtil.escape(filePath)}")
        }
    }
}
