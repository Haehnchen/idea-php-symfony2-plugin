package fr.adrienbrault.idea.symfony2plugin.tests.routing;

import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.routing.Route
 */
public class RouteTest extends Assert {

    @Test
    public void testIndexInit() {
        Route route = new Route("foo", new String[] {"foo", "foo_1"});
        assertEquals("foo", route.getController());
        assertEquals("foo_1", route.getPath());

        route = new Route("foo", new String[] {null, "foo_1"});
        assertEquals("foo_1", route.getPath());

        route = new Route("foo", new String[] {"foo"});
        assertEquals("foo", route.getController());
    }

    @Test
    public void testIndexNullable() {
        Route route = new Route("foo", new String[] {null, null});
        assertNull(route.getController());
        assertNull(route.getPath());

        route = new Route("foo", new String[] {"", ""});
        assertNull(route.getController());
        assertNull(route.getPath());

        route = new Route("foo", new String[] {});
        assertEquals("foo", route.getName());
        assertNull(route.getController());
        assertNull(route.getPath());
    }
}
