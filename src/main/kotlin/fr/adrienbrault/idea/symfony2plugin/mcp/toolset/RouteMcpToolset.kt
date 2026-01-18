@file:Suppress("FunctionName", "unused")

package fr.adrienbrault.idea.symfony2plugin.mcp.toolset

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.mcpserver.project
import com.intellij.openapi.application.readAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.php.PhpIndex
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.mcp.McpUtil
import fr.adrienbrault.idea.symfony2plugin.routing.Route
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil
import kotlinx.coroutines.currentCoroutineContext

/**
 * MCP toolset for Symfony routing information.
 * Provides access to routes configured in the Symfony project.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class RouteMcpToolset : McpToolset {

    @McpTool
    @McpDescription("""
        Lists Symfony routes with their controller mappings and file paths.

        Use this tool to find routes and their associated controllers in the Symfony project.
        This is essential for understanding the relationship between URL paths, route names,
        controller classes, and their physical file locations.

        Key features:
        - Search routes by name or controller
        - Returns controller class and method for each route
        - Provides file path to controller implementation
        - Supports partial matching for flexible searches

        Returns CSV format with columns: name,controller,path,filePath
        - name: Route name
        - controller: Full controller class::method reference
        - path: URL path pattern (may contain parameters like {id})
        - filePath: Relative path to controller file from project root

        Example output:
        name,controller,path,filePath
        app_home,App\Controller\HomeController::index,/,src/Controller/HomeController.php
        api_users_list,App\Controller\UserController::list,/api/users,src/Controller/UserController.php
        api_users_show,App\Controller\UserController::show,/api/users/{id},src/Controller/UserController.php
    """)
    suspend fun list_symfony_routes_controllers(
        @McpDescription("Optional: Filter by route name (supports partial matching, case-insensitive)")
        routeName: String? = null,
        @McpDescription("Optional: Filter by controller class or method (supports partial matching, case-insensitive)")
        controller: String? = null
    ): String {
        val project = currentCoroutineContext().project

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            mcpFail("Symfony plugin is not enabled for this project.")
        }

        McpUtil.checkToolEnabled(project, "list_symfony_routes_controllers")

        return readAction {
            val allRoutes = RouteHelper.getAllRoutes(project)
            val phpIndex = PhpIndex.getInstance(project)
            val projectDir = ProjectUtil.getProjectDir(project)

            val csv = StringBuilder("name,controller,path,filePath\n")

            allRoutes
                .filter { (name, route) ->
                    val matchesName = routeName == null || name.contains(routeName, ignoreCase = true)
                    val matchesController = controller == null ||
                        route.controller?.contains(controller, ignoreCase = true) == true
                    matchesName && matchesController
                }
                .forEach { (_, route) ->
                    val controllerClass = route.controller?.let { ctrl ->
                        if (ctrl.contains("::")) ctrl.substringBefore("::") else ctrl
                    }

                    val filePath = controllerClass?.let { className ->
                        val fqn = if (className.startsWith("\\")) className else "\\$className"
                        phpIndex.getClassesByFQN(fqn).firstOrNull()
                            ?.containingFile
                            ?.virtualFile
                            ?.let { virtualFile ->
                                projectDir?.let { dir ->
                                    VfsUtil.getRelativePath(virtualFile, dir, '/')
                                        ?: FileUtil.getRelativePath(dir.path, virtualFile.path, '/')
                                }
                            }
                    } ?: ""

                    csv.append("${escapeCsv(route.name)},${escapeCsv(route.controller ?: "")},${escapeCsv(route.path ?: "")},${escapeCsv(filePath)}\n")
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
