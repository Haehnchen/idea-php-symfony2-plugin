package fr.adrienbrault.idea.symfony2plugin.tests.routing;

import com.google.common.collect.ImmutableMap;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.StubIndexedRoute;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

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

    @Test
    public void testControllerNullable() {
        assertNull(new Route("foo", new HashSet<>(), new HashMap<>() {{
            put("_controller", null);
        }}, new HashMap<>(), new ArrayList<>()).getController());

        assertEquals("foobar", new Route(
            "foo", new HashSet<>(), ImmutableMap.of("_controller", "foobar"), new HashMap<>(), new ArrayList<>()).getController()
        );
    }

    @Test
    public void testPathVariables() {
        StubIndexedRoute route = new StubIndexedRoute("foobar");
        route.setPath("/foo/{foo}/{foobar}/{#foo1}/{|foo2}/bar");

        Set<String> variables = new Route(route).getVariables();
        assertEquals(2, variables.size());
        assertTrue("foobar", variables.containsAll(Arrays.asList("foo", "foobar")));

        HashSet<String> variables1 = new HashSet<>();
        variables1.add("foobar");

        Set<String> variables2 = new Route("foobar_route", variables1, new HashMap<>(), new HashMap<>(), new ArrayList<>(), "/test/{car}").getVariables();
        assertArrayEquals(new String[] {"car", "foobar"}, variables2.toArray());

        Set<String> variables3 = new Route("foobar_route", variables1, new HashMap<>(), new HashMap<>(), new ArrayList<>(), null).getVariables();
        assertArrayEquals(new String[] {"foobar"}, variables3.toArray());
    }

    @Test
    public void testPathVariablesForDefault() {
        StubIndexedRoute route = new StubIndexedRoute("foobar");
        route.setPath("/foo/{!foo}/{!foobar}//{Foobar2}bar");

        Set<String> variables = new Route(route).getVariables();
        assertEquals(3, variables.size());
        assertTrue(variables.containsAll(Arrays.asList("foo", "foobar", "Foobar2")));
    }

    @Test
    public void testPathVariablesForWildcardInlineRequirements() {
        StubIndexedRoute route = new StubIndexedRoute("foobar");
        route.setPath("/foo/{!foo<.*>}/aaa{foobar<.*>}aaa/{page<\\d+>}/{page2<\\d+}>}/aaaa/{page3<\\{}d+}>}/");

        Set<String> variables = new Route(route).getVariables();
        assertEquals(5, variables.size());

        assertTrue(variables.containsAll(Arrays.asList("foo", "foobar", "page", "page2", "page3")));
    }
}
