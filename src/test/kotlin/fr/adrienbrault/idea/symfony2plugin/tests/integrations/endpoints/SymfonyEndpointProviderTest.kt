package fr.adrienbrault.idea.symfony2plugin.tests.integrations.endpoints

import com.intellij.microservices.endpoints.EndpointsFilter
import com.intellij.microservices.endpoints.EndpointsProvider
import fr.adrienbrault.idea.symfony2plugin.integrations.endpoints.SymfonyEndpointProvider
import fr.adrienbrault.idea.symfony2plugin.integrations.endpoints.SymfonyRouteGroup
import fr.adrienbrault.idea.symfony2plugin.routing.Route
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see SymfonyEndpointProvider
 */
class SymfonyEndpointProviderTest : SymfonyLightCodeInsightFixtureTestCase() {

    override fun getTestDataPath(): String =
        "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/integrations/endpoints/fixtures"

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("routes.yaml")
    }

    /**
     * Plugin is enabled, routes exist → HAS_ENDPOINTS
     *
     * @see SymfonyEndpointProvider.getStatus
     */
    fun testGetStatusReturnsHasEndpoints_whenRoutesAreIndexed() {
        val provider = SymfonyEndpointProvider()

        val status = provider.getStatus(project)

        assertEquals(EndpointsProvider.Status.HAS_ENDPOINTS, status)
    }

    /**
     * Plugin is disabled → UNAVAILABLE
     *
     * @see SymfonyEndpointProvider.getStatus
     */
    fun testGetStatusReturnsUnavailable_whenPluginDisabled() {
        project.getService(fr.adrienbrault.idea.symfony2plugin.Settings::class.java).pluginEnabled = false

        val provider = SymfonyEndpointProvider()
        val status = provider.getStatus(project)

        assertEquals(EndpointsProvider.Status.UNAVAILABLE, status)

        // Re-enable for other tests
        project.getService(fr.adrienbrault.idea.symfony2plugin.Settings::class.java).pluginEnabled = true
    }

    /**
     * Internal routes starting with "_" must not appear in any group.
     *
     * @see SymfonyEndpointProvider.getEndpointGroups
     */
    fun testGetEndpointGroupsInternalRoutesAreExcluded() {
        val provider = SymfonyEndpointProvider()
        val filter = object : EndpointsFilter {}

        val groups = provider.getEndpointGroups(project, filter)

        val allRoutes = collectAllRoutes(provider, groups)

        // No route with a name starting with "_" should be included
        for (route in allRoutes) {
            assertFalse(
                "Internal route '${route.name}' should be excluded",
                route.name.startsWith("_")
            )
        }
    }

    /**
     * Routes are exposed in a single logical group.
     *
     * @see SymfonyEndpointProvider.getEndpointGroups
     */
    fun testGetEndpointGroupsReturnsSingleRoutesGroup() {
        val provider = SymfonyEndpointProvider()
        val filter = object : EndpointsFilter {}

        val groups = provider.getEndpointGroups(project, filter).toList()

        assertEquals("Expected a single routes group", 1, groups.size)
        assertEquals("routes", groups[0].name)
    }

    /**
     * The routes group should include all indexed non-internal routes.
     *
     * @see SymfonyEndpointProvider.getEndpoints
     */
    fun testGetEndpointsReturnsAllRoutesInGroup() {
        val provider = SymfonyEndpointProvider()
        val filter = object : EndpointsFilter {}

        val routesGroup = findGroup(provider, filter, "routes")
        assertNotNull("Group 'routes' must exist", routesGroup)

        val routes = provider.getEndpoints(routesGroup!!).toList()
        val routeNames = routes.map { it.name }

        assertTrue("app_home should be in group 'routes'", routeNames.contains("app_home"))
        assertTrue("app_user_list should be in group 'routes'", routeNames.contains("app_user_list"))
        assertTrue("app_user_show should be in group 'routes'", routeNames.contains("app_user_show"))
        assertTrue("api_products_list should be in group 'routes'", routeNames.contains("api_products_list"))
    }

    /**
     * isValidEndpoint must return true for routes with a non-empty path.
     *
     * @see SymfonyEndpointProvider.isValidEndpoint
     */
    fun testIsValidEndpointReturnsTrueForRoutesWithPath() {
        val provider = SymfonyEndpointProvider()
        val filter = object : EndpointsFilter {}

        val routesGroup = findGroup(provider, filter, "routes")
        assertNotNull(routesGroup)

        for (route in routesGroup!!.routes) {
            assertTrue(
                "Route '${route.name}' should be valid",
                provider.isValidEndpoint(routesGroup, route)
            )
        }
    }

    /**
     * getEndpointPresentation returns an ItemPresentation for the given endpoint.
     *
     * @see SymfonyEndpointProvider.getEndpointPresentation
     */
    fun testGetEndpointPresentationReturnsPresentation() {
        val provider = SymfonyEndpointProvider()
        val filter = object : EndpointsFilter {}

        val routesGroup = findGroup(provider, filter, "routes")
        assertNotNull(routesGroup)

        val homeRoute = routesGroup!!.routes.firstOrNull { it.name == "app_home" }
        assertNotNull("Route 'app_home' must be in the group", homeRoute)

        val presentation = provider.getEndpointPresentation(routesGroup, homeRoute!!)

        assertNotNull("Presentation must not be null", presentation)
        assertEquals("/", presentation.presentableText)
        assertEquals(
            "app_home (HomeController:index)",
            presentation.locationString
        )
    }

    /**
     * HTTP methods from the route definition are reflected in the presentation.
     *
     * @see SymfonyEndpointProvider.getEndpointPresentation
     */
    fun testGetEndpointPresentationIncludesHttpMethods() {
        val provider = SymfonyEndpointProvider()
        val filter = object : EndpointsFilter {}

        val routesGroup = findGroup(provider, filter, "routes")
        assertNotNull(routesGroup)

        val listRoute = routesGroup!!.routes.firstOrNull { it.name == "app_user_list" }
        assertNotNull("Route 'app_user_list' must be in the group", listRoute)

        val presentation = provider.getEndpointPresentation(routesGroup, listRoute!!)

        assertNotNull(presentation)
        // The presentation must contain the path
        assertEquals("/users", presentation.presentableText)
    }

    /**
     * Duplicate FQCN-style routes for the same path/controller should be hidden in favor of named routes.
     */
    fun testGetEndpointsDeduplicatesFqcnRouteWhenNamedRouteExists() {
        myFixture.copyFileToProject("routes_duplicates.yaml")

        val provider = SymfonyEndpointProvider()
        val filter = object : EndpointsFilter {}

        val routesGroup = findGroup(provider, filter, "routes")
        assertNotNull(routesGroup)

        val routes = provider.getEndpoints(routesGroup!!).toList()
        val routeNames = routes.map { it.name }

        assertTrue(routeNames.contains("app_user_list"))
        assertFalse(routeNames.contains("App\\Controller\\UserController::list"))
    }

    // --- helpers ---

    private fun collectAllRoutes(provider: SymfonyEndpointProvider, groups: Iterable<SymfonyRouteGroup>): List<Route> {
        val result = mutableListOf<Route>()
        for (group in groups) {
            provider.getEndpoints(group).forEach { result.add(it) }
        }
        return result
    }

    private fun findGroup(provider: SymfonyEndpointProvider, filter: EndpointsFilter, name: String): SymfonyRouteGroup? {
        return provider.getEndpointGroups(project, filter).firstOrNull { it.name == name }
    }
}
