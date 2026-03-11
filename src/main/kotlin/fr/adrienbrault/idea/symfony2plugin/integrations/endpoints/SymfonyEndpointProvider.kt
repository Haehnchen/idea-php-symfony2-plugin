package fr.adrienbrault.idea.symfony2plugin.integrations.endpoints

import com.intellij.microservices.endpoints.EndpointType
import com.intellij.microservices.endpoints.EndpointsFilter
import com.intellij.microservices.endpoints.EndpointsProvider
import com.intellij.microservices.endpoints.FrameworkPresentation
import com.intellij.microservices.endpoints.HTTP_SERVER_TYPE
import com.intellij.microservices.endpoints.presentation.HttpMethodPresentation
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiModificationTracker
import com.jetbrains.php.lang.PhpLanguage
import fr.adrienbrault.idea.symfony2plugin.Settings
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import fr.adrienbrault.idea.symfony2plugin.routing.Route
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper

/**
 * Exposes Symfony routes in IntelliJ Ultimate's Endpoints tool window.
 *
 * Routes are exposed in a single group to avoid imposing project-specific naming conventions.
 * Internal routes starting with "_" are excluded.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/endpoints-api.html">Endpoints API</a>
 */
class SymfonyEndpointProvider : EndpointsProvider<SymfonyRouteGroup, Route> {

    override val endpointType: EndpointType get() = HTTP_SERVER_TYPE

    override val presentation: FrameworkPresentation get() = PRESENTATION

    override fun getStatus(project: Project): EndpointsProvider.Status {
        val settings = Settings.getInstance(project) ?: return EndpointsProvider.Status.UNAVAILABLE
        if (!settings.pluginEnabled) return EndpointsProvider.Status.UNAVAILABLE

        val routes = RouteHelper.getAllRoutesUnique(project)
        return if (routes.isEmpty()) EndpointsProvider.Status.AVAILABLE else EndpointsProvider.Status.HAS_ENDPOINTS
    }

    override fun getEndpointGroups(project: Project, filter: EndpointsFilter): Iterable<SymfonyRouteGroup> {
        val settings = Settings.getInstance(project) ?: return emptyList()
        if (!settings.pluginEnabled) return emptyList()

        val allRoutes = RouteHelper.getAllRoutesUnique(project)
        val routes = mutableListOf<Route>()

        for (route in allRoutes.values) {
            // Skip internal Symfony framework routes
            if (route.name.startsWith("_")) continue

            // Skip routes without a path
            val path = route.path
            if (path.isNullOrEmpty()) continue

            routes.add(route)
        }

        if (routes.isEmpty()) return emptyList()

        return listOf(SymfonyRouteGroup(project, ROUTES_GROUP_NAME, routes))
    }

    override fun getEndpoints(group: SymfonyRouteGroup): Iterable<Route> = group.routes

    override fun isValidEndpoint(group: SymfonyRouteGroup, endpoint: Route): Boolean =
        !endpoint.path.isNullOrEmpty()

    override fun getEndpointPresentation(group: SymfonyRouteGroup, endpoint: Route): ItemPresentation {
        val path = endpoint.path
        val methods = endpoint.methods
        val definitionSource = buildDefinitionSource(endpoint)

        return if (methods.isEmpty()) {
            HttpMethodPresentation(path, null as String?, definitionSource, Symfony2Icons.SYMFONY)
        } else {
            HttpMethodPresentation(path, ArrayList(methods), definitionSource, Symfony2Icons.SYMFONY)
        }
    }

    override fun getNavigationElement(group: SymfonyRouteGroup, endpoint: Route): PsiElement? {
        val methods = RouteHelper.getMethods(group.project, endpoint.name)
        return if (methods.isNotEmpty()) methods[0] else null
    }

    override fun getModificationTracker(project: Project): ModificationTracker {
        // FileBasedIndex#getIndexModificationStamp can assert on EDT in Endpoints scrolling/update paths.
        // Use PSI tracker to keep Endpoints refresh safe on EDT.
        return PsiModificationTracker.getInstance(project).forLanguage(PhpLanguage.INSTANCE)
    }

    companion object {
        private const val ROUTES_GROUP_NAME = "routes"

        private val PRESENTATION = FrameworkPresentation(
            "symfony",
            "Symfony",
            Symfony2Icons.SYMFONY
        )

        private fun buildDefinitionSource(endpoint: Route): String {
            val routeName = endpoint.name
            val controller = endpoint.controller

            if (controller.isNullOrEmpty()) return routeName

            return "$routeName (${formatControllerShort(controller)})"
        }

        private fun formatControllerShort(controller: String): String {
            if (controller.contains("::")) {
                val parts = controller.split("::", limit = 2)
                return "${shortClassName(parts[0])}:${parts[1]}"
            }

            val colonIndex = controller.lastIndexOf(':')
            if (colonIndex > 0 && colonIndex < controller.length - 1) {
                val left = controller.substring(0, colonIndex)
                val right = controller.substring(colonIndex + 1)
                return "${shortClassName(left)}:$right"
            }

            return shortClassName(controller)
        }

        private fun shortClassName(value: String): String {
            val namespaceSeparator = value.lastIndexOf('\\')
            if (namespaceSeparator >= 0 && namespaceSeparator < value.length - 1) {
                return value.substring(namespaceSeparator + 1)
            }
            return value
        }
    }
}
