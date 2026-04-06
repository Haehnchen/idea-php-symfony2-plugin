package fr.adrienbrault.idea.symfony2plugin.mcp.collector

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil
import fr.adrienbrault.idea.symfony2plugin.mcp.McpGlobMatcher
import fr.adrienbrault.idea.symfony2plugin.mcp.McpPathUtil
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.Method
import fr.adrienbrault.idea.symfony2plugin.routing.Route
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import org.apache.commons.lang3.StringUtils

class SymfonyRouteCollector(private val project: Project) {
    fun collect(
        routeName: String? = null,
        controller: String? = null,
        urlPath: String? = null,
        fileGlob: String? = null
    ): String = buildString {
        val allRoutes = RouteHelper.getAllRoutes(project)
        val phpIndex = PhpIndex.getInstance(project)
        val normalizedUrlPath = urlPath?.trim()?.takeIf { it.isNotBlank() }
        val matchingRouteNames = normalizedUrlPath?.let {
            RouteHelper.getMethodsForPathWithPlaceholderMatchRoutes(project, it)
                .map { (route, _) -> route.name }
                .toSet()
        } ?: emptySet()

        val filteredRoutes = allRoutes.filter { (name, route) ->
            val matchesName = routeName == null || name.contains(routeName, ignoreCase = true)
            val matchesController = controller == null || route.controller?.contains(controller, ignoreCase = true) == true
            val matchesUrl = normalizedUrlPath == null || route.matchesUrl(normalizedUrlPath, matchingRouteNames)
            matchesName && matchesController && matchesUrl
        }

        val controllerFqns = filteredRoutes
            .mapNotNull { it.value.controller }
            .filter { "::" in it }
            .toSet()

        val templatesByController = TwigUtil.findTemplatesByControllers(project, controllerFqns)

        appendLine("name,controller,path,filePath,lineNumber,templates")

        filteredRoutes.forEach { (_, route) ->
            val controllerInfo = route.controller?.takeIf { "::" in it }?.let { controller ->
                val className = controller.substringBefore("::")
                val methodName = controller.substringAfter("::")
                Triple(controller, className, methodName)
            }

            val controllerClass = controllerInfo?.second ?: route.controller?.let { ctrl ->
                if ("::" in ctrl) ctrl.substringBefore("::") else ctrl
            }

            val controllerMethod = controllerInfo?.let { (_, className, methodName) ->
                getControllerMethod(className, methodName)
            }

            val filePath = controllerClass?.let { className ->
                val fqn = if (className.startsWith("\\")) className else "\\$className"
                phpIndex.getClassesByFQN(fqn).firstOrNull()
                    ?.containingFile
                    ?.virtualFile
                    ?.let { McpPathUtil.getRelativeProjectPath(project, it) }
            } ?: ""

            val normalizedFileGlob = fileGlob?.trim()?.takeIf { it.isNotBlank() }
            if (normalizedFileGlob != null && !McpGlobMatcher.matches(filePath, normalizedFileGlob)) {
                return@forEach
            }

            val lineNumber = controllerMethod?.let { getLineNumber(it).toString() } ?: ""

            val normalizedController = route.controller
                ?.replace("::", ".")
                ?.let { StringUtils.stripStart(it, "\\") }

            val templates = normalizedController?.let { ctrl ->
                templatesByController[ctrl]?.joinToString(";") ?: ""
            } ?: ""

            appendLine("${McpCsvUtil.escape(route.name)},${McpCsvUtil.escape(route.controller ?: "")},${McpCsvUtil.escape(route.path ?: "")},${McpCsvUtil.escape(filePath)},${McpCsvUtil.escape(lineNumber)},${McpCsvUtil.escape(templates)}")
        }
    }

    private fun getControllerMethod(className: String, methodName: String): Method? {
        val fqn = if (className.startsWith("\\")) className else "\\$className"

        return PhpIndex.getInstance(project)
            .getClassesByFQN(fqn)
            .asSequence()
            .mapNotNull { it.findMethodByName(methodName) }
            .firstOrNull()
    }

    private fun getLineNumber(method: Method): Int {
        val file = method.containingFile ?: return 1
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return 1

        return document.getLineNumber(method.textRange.startOffset) + 1
    }

    private fun Route.matchesUrl(urlPath: String, matchingRouteNames: Set<String>): Boolean {
        val routePath = this.path ?: return false

        return routePath.contains(urlPath, ignoreCase = true)
            || urlPath.contains(routePath, ignoreCase = true)
            || matchingRouteNames.contains(this.name)
    }
}
