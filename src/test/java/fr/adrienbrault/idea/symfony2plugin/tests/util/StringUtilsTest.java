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
}
