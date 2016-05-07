package fr.adrienbrault.idea.symfony2plugin.tests.routing;

import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.StubIndexedRoute;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.routing.Route
 */
public class RouteTest extends Assert {

    @Test
    public void testIndexInit() {
        StubIndexedRoute stubIndexedRoute = new StubIndexedRoute("foo");
        stubIndexedRoute.setPath("foo_1");
        stubIndexedRoute.setController("foo");

        Route route = new Route(stubIndexedRoute);
        assertEquals("foo", route.getController());
        assertEquals("foo_1", route.getPath());
    }

    @Test
    public void testIndexNullable() {
        StubIndexedRoute stubIndexedRoute = new StubIndexedRoute("foo");
        Route route = new Route(stubIndexedRoute);
        assertNull(route.getController());
        assertNull(route.getPath());
    }
}
