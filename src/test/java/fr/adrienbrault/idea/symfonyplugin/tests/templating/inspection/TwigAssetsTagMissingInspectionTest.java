package fr.adrienbrault.idea.symfonyplugin.tests.templating.inspection;

import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfonyplugin.templating.inspection.TwigAssetMissingInspection
 */
public class TwigAssetsTagMissingInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testThatUnknownAssetIsHighlighted() {
        assertLocalInspectionContains(
            "test.html.twig",
            "" +
                "{% javascripts\n" +
                "    'foo<caret>bar.js'" +
                " %}\n" +
                "    <script src=\"{{ asset_url }}\"></script>\n" +
                "{% endjavascripts %}",
            "Missing asset"
        );

        assertLocalInspectionContains(
            "test.html.twig",
            "" +
                "{% stylesheets\n" +
                "    \"color<caret>box.css\"\n" +
                "%}\n" +
                "<link type=\"text/css\" rel=\"stylesheet\" media=\"all\" href=\"{{ asset_url }}\" />\n" +
                "{% endstylesheets %}",
            "Missing asset"
        );
    }

    public void testThatInvalidStringMustNotHighlight() {
        assertLocalInspectionNotContains(
            "test.html.twig",
            "" +
                "{% javascripts\n" +
                "    'foo' ~ 'foo<caret>bar.js'" +
                " %}\n" +
                "    <script src=\"{{ asset_url }}\"></script>\n" +
                "{% endjavascripts %}",
            "Missing asset"
        );

        assertLocalInspectionNotContains(
            "test.html.twig",
            "" +
                "{% javascripts\n" +
                "    'f<caret>oo' ~ 'foobar.js'" +
                " %}\n" +
                "    <script src=\"{{ asset_url }}\"></script>\n" +
                "{% endjavascripts %}",
            "Missing asset"
        );

        assertLocalInspectionNotContains(
            "test.html.twig",
            "" +
                "{% javascripts\n" +
                "    'fo#{foo}ob<caret>ar.js'" +
                " %}\n" +
                "    <script src=\"{{ asset_url }}\"></script>\n" +
                "{% endjavascripts %}",
            "Missing asset"
        );
    }
}
