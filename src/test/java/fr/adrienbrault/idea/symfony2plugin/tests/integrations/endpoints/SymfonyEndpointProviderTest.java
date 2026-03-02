package fr.adrienbrault.idea.symfony2plugin.tests.integrations.endpoints;

import com.intellij.microservices.endpoints.EndpointsFilter;
import com.intellij.microservices.endpoints.EndpointsProvider;
import fr.adrienbrault.idea.symfony2plugin.integrations.endpoints.SymfonyEndpointProvider;
import fr.adrienbrault.idea.symfony2plugin.integrations.endpoints.SymfonyRouteGroup;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see SymfonyEndpointProvider
 */
public class SymfonyEndpointProviderTest extends SymfonyLightCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/integrations/endpoints/fixtures";
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("routes.yaml");
    }

    /**
     * Plugin is enabled, routes exist → HAS_ENDPOINTS
     *
     * @see SymfonyEndpointProvider#getStatus
     */
    public void testGetStatusReturnsHasEndpoints_whenRoutesAreIndexed() {
        SymfonyEndpointProvider provider = new SymfonyEndpointProvider();

        EndpointsProvider.Status status = provider.getStatus(getProject());

        assertEquals(EndpointsProvider.Status.HAS_ENDPOINTS, status);
    }

    /**
     * Plugin is disabled → UNAVAILABLE
     *
     * @see SymfonyEndpointProvider#getStatus
     */
    public void testGetStatusReturnsUnavailable_whenPluginDisabled() {
        getProject().getService(fr.adrienbrault.idea.symfony2plugin.Settings.class).pluginEnabled = false;

        SymfonyEndpointProvider provider = new SymfonyEndpointProvider();
        EndpointsProvider.Status status = provider.getStatus(getProject());

        assertEquals(EndpointsProvider.Status.UNAVAILABLE, status);

        // Re-enable for other tests
        getProject().getService(fr.adrienbrault.idea.symfony2plugin.Settings.class).pluginEnabled = true;
    }

    /**
     * Internal routes starting with "_" must not appear in any group.
     *
     * @see SymfonyEndpointProvider#getEndpointGroups
     */
    public void testGetEndpointGroupsInternalRoutesAreExcluded() {
        SymfonyEndpointProvider provider = new SymfonyEndpointProvider();
        EndpointsFilter filter = new EndpointsFilter() {};

        Iterable<SymfonyRouteGroup> groups = provider.getEndpointGroups(getProject(), filter);

        List<Route> allRoutes = collectAllRoutes(provider, groups);

        // No route with a name starting with "_" should be included
        for (Route route : allRoutes) {
            assertFalse(
                "Internal route '" + route.getName() + "' should be excluded",
                route.getName().startsWith("_")
            );
        }
    }

    /**
     * Routes are exposed in a single logical group.
     *
     * @see SymfonyEndpointProvider#getEndpointGroups
     */
    public void testGetEndpointGroupsReturnsSingleRoutesGroup() {
        SymfonyEndpointProvider provider = new SymfonyEndpointProvider();
        EndpointsFilter filter = new EndpointsFilter() {};

        List<SymfonyRouteGroup> groups = new ArrayList<>();
        provider.getEndpointGroups(getProject(), filter).forEach(groups::add);

        assertEquals("Expected a single routes group", 1, groups.size());
        assertEquals("routes", groups.get(0).getName());
    }

    /**
     * The routes group should include all indexed non-internal routes.
     *
     * @see SymfonyEndpointProvider#getEndpoints
     */
    public void testGetEndpointsReturnsAllRoutesInGroup() {
        SymfonyEndpointProvider provider = new SymfonyEndpointProvider();
        EndpointsFilter filter = new EndpointsFilter() {};

        SymfonyRouteGroup routesGroup = findGroup(provider, filter, "routes");
        assertNotNull("Group 'routes' must exist", routesGroup);

        List<Route> routes = new ArrayList<>();
        provider.getEndpoints(routesGroup).forEach(routes::add);

        List<String> routeNames = routes.stream().map(Route::getName).toList();

        assertTrue("app_home should be in group 'routes'", routeNames.contains("app_home"));
        assertTrue("app_user_list should be in group 'routes'", routeNames.contains("app_user_list"));
        assertTrue("app_user_show should be in group 'routes'", routeNames.contains("app_user_show"));
        assertTrue("api_products_list should be in group 'routes'", routeNames.contains("api_products_list"));
    }

    /**
     * isValidEndpoint must return true for routes with a non-empty path.
     *
     * @see SymfonyEndpointProvider#isValidEndpoint
     */
    public void testIsValidEndpointReturnsTrueForRoutesWithPath() {
        SymfonyEndpointProvider provider = new SymfonyEndpointProvider();
        EndpointsFilter filter = new EndpointsFilter() {};

        SymfonyRouteGroup routesGroup = findGroup(provider, filter, "routes");
        assertNotNull(routesGroup);

        for (Route route : routesGroup.getRoutes()) {
            assertTrue(
                "Route '" + route.getName() + "' should be valid",
                provider.isValidEndpoint(routesGroup, route)
            );
        }
    }

    /**
     * getEndpointPresentation returns an ItemPresentation for the given endpoint.
     *
     * @see SymfonyEndpointProvider#getEndpointPresentation
     */
    public void testGetEndpointPresentationReturnsPresentation() {
        SymfonyEndpointProvider provider = new SymfonyEndpointProvider();
        EndpointsFilter filter = new EndpointsFilter() {};

        SymfonyRouteGroup routesGroup = findGroup(provider, filter, "routes");
        assertNotNull(routesGroup);

        Route homeRoute = routesGroup.getRoutes().stream()
            .filter(r -> r.getName().equals("app_home"))
            .findFirst()
            .orElse(null);
        assertNotNull("Route 'app_home' must be in the group", homeRoute);

        var presentation = provider.getEndpointPresentation(routesGroup, homeRoute);

        assertNotNull("Presentation must not be null", presentation);
        assertEquals("/", presentation.getPresentableText());
        assertEquals(
            "app_home (HomeController:index)",
            presentation.getLocationString()
        );
    }

    /**
     * HTTP methods from the route definition are reflected in the presentation.
     *
     * @see SymfonyEndpointProvider#getEndpointPresentation
     */
    public void testGetEndpointPresentationIncludesHttpMethods() {
        SymfonyEndpointProvider provider = new SymfonyEndpointProvider();
        EndpointsFilter filter = new EndpointsFilter() {};

        SymfonyRouteGroup routesGroup = findGroup(provider, filter, "routes");
        assertNotNull(routesGroup);

        Route listRoute = routesGroup.getRoutes().stream()
            .filter(r -> r.getName().equals("app_user_list"))
            .findFirst()
            .orElse(null);
        assertNotNull("Route 'app_user_list' must be in the group", listRoute);

        var presentation = provider.getEndpointPresentation(routesGroup, listRoute);

        assertNotNull(presentation);
        // The presentation must contain the path
        assertEquals("/users", presentation.getPresentableText());
    }

    /**
     * Duplicate FQCN-style routes for the same path/controller should be hidden in favor of named routes.
     */
    public void testGetEndpointsDeduplicatesFqcnRouteWhenNamedRouteExists() {
        myFixture.copyFileToProject("routes_duplicates.yaml");

        SymfonyEndpointProvider provider = new SymfonyEndpointProvider();
        EndpointsFilter filter = new EndpointsFilter() {};

        SymfonyRouteGroup routesGroup = findGroup(provider, filter, "routes");
        assertNotNull(routesGroup);

        List<Route> routes = new ArrayList<>();
        provider.getEndpoints(routesGroup).forEach(routes::add);

        List<String> routeNames = routes.stream().map(Route::getName).toList();
        assertTrue(routeNames.contains("app_user_list"));
        assertFalse(routeNames.contains("App\\Controller\\UserController::list"));
    }

    // --- helpers ---

    private List<Route> collectAllRoutes(SymfonyEndpointProvider provider, Iterable<SymfonyRouteGroup> groups) {
        List<Route> result = new ArrayList<>();
        for (SymfonyRouteGroup group : groups) {
            provider.getEndpoints(group).forEach(result::add);
        }
        return result;
    }

    private SymfonyRouteGroup findGroup(SymfonyEndpointProvider provider, EndpointsFilter filter, String name) {
        return StreamSupport.stream(provider.getEndpointGroups(getProject(), filter).spliterator(), false)
            .filter(g -> g.getName().equals(name))
            .findFirst()
            .orElse(null);
    }
}
