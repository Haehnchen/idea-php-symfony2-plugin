package fr.adrienbrault.idea.symfony2plugin.integrations.endpoints

import com.intellij.openapi.project.Project
import fr.adrienbrault.idea.symfony2plugin.routing.Route

/**
 * Represents a named group of Symfony routes for the Endpoints tool window.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class SymfonyRouteGroup(
    val project: Project,
    val name: String,
    val routes: List<Route>
)
