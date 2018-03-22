package fr.adrienbrault.idea.symfony2plugin.tests.templating.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.inspection.TwigAssetMissingInspection
 */
public class TwigAssetMissingInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testThatUnknownAssetIsHighlighted() {
        assertLocalInspectionContains(
            "test.html.twig",
            "{{ asset('foob<caret>ar.css') %}",
            "Missing asset"
        );

        assertLocalInspectionContains(
            "test.html.twig",
            "{{ asset(\"foob<caret>ar.css\") %}",
            "Missing asset"
        );
    }

    public void testThatInvalidStringIsNotHighlighted() {
        assertLocalInspectionNotContains(
            "test.html.twig",
            "{{ asset('foo#{segment}fo<caret>o') %}",
            "Missing asset"
        );

        assertLocalInspectionNotContains(
            "test.html.twig",
            "{{ asset('f<caret>oo' ~ 'foobar.css') %}",
            "Missing asset"
        );

        assertLocalInspectionNotContains(
            "test.html.twig",
            "{{ asset('foo' ~ 'foob<caret>ar.css') %}",
            "Missing asset"
        );
    }
}
