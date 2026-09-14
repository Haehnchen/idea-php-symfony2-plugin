package fr.adrienbrault.idea.symfony2plugin.tests.templating.inspection

import fr.adrienbrault.idea.symfony2plugin.templating.inspection.TwigAssetMissingInspection
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.inspection.TwigAssetMissingInspection
 */
class TwigAssetMissingInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testThatUnknownAssetIsHighlighted() {
        assertLocalInspectionContains(TwigAssetMissingInspection::class.java,
            "test.html.twig",
            "{{ asset('foob<caret>ar.css') %}",
            "Missing asset"
        )

        assertLocalInspectionContains(TwigAssetMissingInspection::class.java,
            "test.html.twig",
            "{{ asset(\"foob<caret>ar.css\") %}",
            "Missing asset"
        )
    }

    fun testThatInvalidStringIsNotHighlighted() {
        assertLocalInspectionNotContains(TwigAssetMissingInspection::class.java,
            "test.html.twig",
            "{{ asset('foo#{segment}fo<caret>o') %}",
            "Missing asset"
        )

        assertLocalInspectionNotContains(TwigAssetMissingInspection::class.java,
            "test.html.twig",
            "{{ asset('f<caret>oo' ~ 'foobar.css') %}",
            "Missing asset"
        )

        assertLocalInspectionNotContains(TwigAssetMissingInspection::class.java,
            "test.html.twig",
            "{{ asset('foo' ~ 'foob<caret>ar.css') %}",
            "Missing asset"
        )
    }
}
