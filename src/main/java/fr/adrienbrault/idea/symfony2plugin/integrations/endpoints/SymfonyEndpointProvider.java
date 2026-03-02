package fr.adrienbrault.idea.symfony2plugin.integrations.endpoints;

import com.intellij.microservices.endpoints.*;
import com.intellij.microservices.endpoints.presentation.HttpMethodPresentation;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiModificationTracker;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Exposes Symfony routes in IntelliJ Ultimate's Endpoints tool window.
 *
 * Routes are exposed in a single group to avoid imposing project-specific naming conventions.
 * Internal routes starting with "_" are excluded.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/endpoints-api.html">Endpoints API</a>
 */
public class SymfonyEndpointProvider implements EndpointsProvider<SymfonyRouteGroup, Route> {
    private static final String ROUTES_GROUP_NAME = "routes";

    private static final FrameworkPresentation PRESENTATION = new FrameworkPresentation(
        "symfony",
        "Symfony",
        Symfony2Icons.SYMFONY
    );

    @Override
    public @NotNull EndpointType getEndpointType() {
        return EndpointTypes.HTTP_SERVER_TYPE;
    }

    @Override
    public @NotNull FrameworkPresentation getPresentation() {
        return PRESENTATION;
    }

    @Override
    public @NotNull EndpointsProvider.Status getStatus(@NotNull Project project) {
        Settings settings = Settings.getInstance(project);
        if (settings == null || !settings.pluginEnabled) {
            return Status.UNAVAILABLE;
        }

        Map<String, Route> routes = RouteHelper.getAllRoutesUnique(project);
        return routes.isEmpty() ? Status.AVAILABLE : Status.HAS_ENDPOINTS;
    }

    @Override
    public @NotNull Iterable<SymfonyRouteGroup> getEndpointGroups(@NotNull Project project, @NotNull EndpointsFilter filter) {
        Settings settings = Settings.getInstance(project);
        if (settings == null || !settings.pluginEnabled) {
            return Collections.emptyList();
        }

        Map<String, Route> allRoutes = RouteHelper.getAllRoutesUnique(project);
        List<Route> routes = new ArrayList<>();

        for (Route route : allRoutes.values()) {
            String name = route.getName();

            // Skip internal Symfony framework routes
            if (name.startsWith("_")) {
                continue;
            }

            // Skip routes without a path
            String path = route.getPath();
            if (path == null || path.isEmpty()) {
                continue;
            }

            routes.add(route);
        }

        if (routes.isEmpty()) {
            return Collections.emptyList();
        }

        return Collections.singletonList(new SymfonyRouteGroup(project, ROUTES_GROUP_NAME, routes));
    }

    @Override
    public @NotNull Iterable<Route> getEndpoints(@NotNull SymfonyRouteGroup group) {
        return group.getRoutes();
    }

    @Override
    public boolean isValidEndpoint(@NotNull SymfonyRouteGroup group, @NotNull Route endpoint) {
        return endpoint.getPath() != null && !endpoint.getPath().isEmpty();
    }

    @Override
    public @NotNull ItemPresentation getEndpointPresentation(@NotNull SymfonyRouteGroup group, @NotNull Route endpoint) {
        String path = endpoint.getPath();
        Collection<String> methods = endpoint.getMethods();
        String definitionSource = buildDefinitionSource(endpoint);

        if (methods.isEmpty()) {
            return new HttpMethodPresentation(path, (String) null, definitionSource, Symfony2Icons.SYMFONY);
        }

        return new HttpMethodPresentation(path, new ArrayList<>(methods), definitionSource, Symfony2Icons.SYMFONY);
    }

    @Override
    public @Nullable PsiElement getNavigationElement(@NotNull SymfonyRouteGroup group, @NotNull Route endpoint) {
        PsiElement[] methods = RouteHelper.getMethods(group.getProject(), endpoint.getName());
        if (methods.length > 0) {
            return methods[0];
        }

        return null;
    }

    @Override
    public @NotNull ModificationTracker getModificationTracker(@NotNull Project project) {
        // FileBasedIndex#getIndexModificationStamp can assert on EDT in Endpoints scrolling/update paths.
        // Use PSI tracker to keep Endpoints refresh safe on EDT.
        return PsiModificationTracker.getInstance(project).forLanguage(com.jetbrains.php.lang.PhpLanguage.INSTANCE);
    }

    @NotNull
    private static String buildDefinitionSource(@NotNull Route endpoint) {
        String routeName = endpoint.getName();
        String controller = endpoint.getController();

        if (controller == null || controller.isEmpty()) {
            return routeName;
        }

        return routeName + " (" + formatControllerShort(controller) + ")";
    }

    @NotNull
    private static String formatControllerShort(@NotNull String controller) {
        if (controller.contains("::")) {
            String[] parts = controller.split("::", 2);
            return shortClassName(parts[0]) + ":" + parts[1];
        }

        int colonIndex = controller.lastIndexOf(':');
        if (colonIndex > 0 && colonIndex < controller.length() - 1) {
            String left = controller.substring(0, colonIndex);
            String right = controller.substring(colonIndex + 1);
            return shortClassName(left) + ":" + right;
        }

        return shortClassName(controller);
    }

    @NotNull
    private static String shortClassName(@NotNull String value) {
        int namespaceSeparator = value.lastIndexOf('\\');
        if (namespaceSeparator >= 0 && namespaceSeparator < value.length() - 1) {
            return value.substring(namespaceSeparator + 1);
        }

        return value;
    }

}
