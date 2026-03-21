package fr.adrienbrault.idea.symfony2plugin.tests.util;

import fr.adrienbrault.idea.symfony2plugin.util.StringUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class StringUtilsTest extends Assert {
    @Test
    public void testIsInterpolatedString() {
        assertTrue(StringUtils.isInterpolatedString("foo/#{segment.typeKey}.html.twig"));
        assertTrue(StringUtils.isInterpolatedString("foo/#{1 + 2}.html.twig"));

        assertFalse(StringUtils.isInterpolatedString("foo/{foobar}.html.twig"));
        assertFalse(StringUtils.isInterpolatedString("foo/foobar.html.twig"));
    }

    @Test
    public void testRemoveEnd() {
        assertEquals("Foo", StringUtils.removeEnd("FooAction", "Action"));
        assertEquals("Foo", StringUtils.removeEnd("Foo", ""));
        assertEquals("FooBar", StringUtils.removeEnd("FooBar", "action"));
        assertEquals("", StringUtils.removeEnd("Action", "Action"));
        assertEquals("Foo", StringUtils.removeEnd("Foo", "Bar"));
    }

    @Test
    public void testRemoveEndIgnoreCase() {
        assertEquals("Foo", StringUtils.removeEndIgnoreCase("FooAction", "action"));
        assertEquals("Foo", StringUtils.removeEndIgnoreCase("FooACTION", "Action"));
        assertEquals("Foo", StringUtils.removeEndIgnoreCase("Foo", ""));
        assertEquals("Foo", StringUtils.removeEndIgnoreCase("FooBar", "BAR"));
        assertEquals("", StringUtils.removeEndIgnoreCase("action", "ACTION"));
        assertEquals("Foo", StringUtils.removeEndIgnoreCase("Foo", "xyz"));
    }

    @Test
    public void testRemoveStartIgnoreCase() {
        assertEquals("Bar", StringUtils.removeStartIgnoreCase("fooBar", "foo"));
        assertEquals("Bar", StringUtils.removeStartIgnoreCase("FOOBar", "Foo"));
        assertEquals("Bar", StringUtils.removeStartIgnoreCase("Bar", ""));
        assertEquals("FooBar", StringUtils.removeStartIgnoreCase("FooBar", "xyz"));
        assertEquals("", StringUtils.removeStartIgnoreCase("action", "ACTION"));
    }

    @Test
    public void testGetFuzzyDistance() {
        // exact match
        assertTrue(StringUtils.getFuzzyDistance("foo", "foo") > 0);

        // no match
        assertEquals(0, StringUtils.getFuzzyDistance("foo", "xyz"));

        // consecutive matches score higher
        int consecutive = StringUtils.getFuzzyDistance("apache", "apa");
        int scattered = StringUtils.getFuzzyDistance("apache", "ace");
        assertTrue(consecutive > scattered);

        // case insensitive
        assertEquals(
            StringUtils.getFuzzyDistance("Apache", "apache"),
            StringUtils.getFuzzyDistance("apache", "apache")
        );

        // adjacent match bonus: "abc" vs "ab" should score higher than "axc" vs "ac"
        int adjacent = StringUtils.getFuzzyDistance("abc", "ab");
        int nonAdjacent = StringUtils.getFuzzyDistance("axc", "ac");
        assertTrue(adjacent > nonAdjacent);

        // empty query
        assertEquals(0, StringUtils.getFuzzyDistance("foo", ""));
    }
}
