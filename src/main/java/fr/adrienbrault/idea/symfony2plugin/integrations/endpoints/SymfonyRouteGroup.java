package fr.adrienbrault.idea.symfony2plugin.integrations.endpoints;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a named group of Symfony routes for the Endpoints tool window.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyRouteGroup {

    private final @NotNull Project project;
    private final @NotNull String name;
    private final @NotNull List<Route> routes;

    public SymfonyRouteGroup(@NotNull Project project, @NotNull String name, @NotNull List<Route> routes) {
        this.project = project;
        this.name = name;
        this.routes = routes;
    }

    public @NotNull Project getProject() {
        return project;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull List<Route> getRoutes() {
        return routes;
    }
}
