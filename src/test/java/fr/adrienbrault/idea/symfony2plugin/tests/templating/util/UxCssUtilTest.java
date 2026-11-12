package fr.adrienbrault.idea.symfony2plugin.tests.templating.util;

import fr.adrienbrault.idea.symfony2plugin.templating.util.UxCssUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.awt.*;

/**
 * @see UxCssUtil
 */
public class UxCssUtilTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testSharedColorValues() {
        myFixture.addFileToProject("config/reference_css.php", """
            <?php
            namespace Symfony\\UX\\Css;
            final class CssReference {
                public const TOKENS = [
                    'colors' => ['blue' => '#123456', 'primary' => '#123456', 'current' => 'currentColor'],
                    'spacing' => ['primary' => '#123456'],
                ];
            }
            """);

        var tokens = UxCssUtil.getTokens(getProject());
        assertEquals(new Color(0x123456), tokens.get("colors").get("blue").color());
        assertEquals(tokens.get("colors").get("blue"), tokens.get("colors").get("primary"));
        assertEquals("currentColor", tokens.get("colors").get("current").value());
        assertNull(tokens.get("colors").get("current").color());
        assertNull(tokens.get("spacing").get("primary").color());
    }

    public void testParseHexColors() {
        assertEquals(new Color(0x3b, 0x82, 0xf6), parseColor("#3b82f6"));
        assertEquals(new Color(0x3b, 0x82, 0xf6), parseColor("#3B82F6"));
        assertEquals(new Color(0xff, 0xff, 0xff), parseColor("#fff"));
        assertEquals(new Color(0x11, 0x22, 0x33, 0x44), parseColor("#1234"));
        assertEquals(new Color(0x3b, 0x82, 0xf6, 0x80), parseColor("#3b82f680"));
    }

    public void testParseRgbColors() {
        assertEquals(new Color(255, 0, 0), parseColor("rgb(255, 0, 0)"));
        assertEquals(new Color(255, 0, 0), parseColor("rgb(255 0 0)"));
        assertEquals(new Color(0, 0, 0, 0), parseColor("rgb(0 0 0 / 0)"));
        assertColor(new Color(255, 0, 0, 128), "rgba(255, 0, 0, 0.5)");
        assertColor(new Color(255, 0, 0, 128), "rgb(255 0 0 / 50%)");
    }

    public void testParseModernCssColors() {
        assertColor(new Color(255, 0, 0), "hsl(0 100% 50%)");
        assertColor(new Color(255, 0, 0), "oklch(62.8% 0.2577 29.23)");
        assertColor(new Color(0x66, 0x33, 0x99), "rebeccapurple");
        assertColor(new Color(128, 0, 128), "color-mix(in srgb, #f00 50%, #00f)");
        assertColor(new Color(255, 0, 0, 128), "color-mix(in srgb, #f00 50%, transparent)");
        assertColor(new Color(0, 0, 0, 0), "transparent");
    }

    public void testValuesThatAreNotColors() {
        assertNull(parseColor("currentColor"));
        assertNull(parseColor("var(--colors-blue-500)"));
        assertNull(parseColor("1rem"));
        assertNull(parseColor("#12"));
        assertNull(parseColor("#ggg"));
        assertNull(parseColor("rgb(255, 0)"));
        assertNull(parseColor("nope"));
    }

    private Color parseColor(String value) {
        return UxCssUtil.parseColor(getProject(), value);
    }

    private void assertColor(Color expected, String value) {
        Color actual = parseColor(value);
        assertNotNull(value, actual);
        for (int[] channel : new int[][] {
            {expected.getRed(), actual.getRed()},
            {expected.getGreen(), actual.getGreen()},
            {expected.getBlue(), actual.getBlue()},
            {expected.getAlpha(), actual.getAlpha()},
        }) {
            assertTrue(value + ": " + expected + " != " + actual + " alpha=" + actual.getAlpha(), Math.abs(channel[0] - channel[1]) <= 2);
        }
    }
}
