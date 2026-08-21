package fr.adrienbrault.idea.symfony2plugin.tests.routing.inspection

import fr.adrienbrault.idea.symfony2plugin.routing.inspection.TwigRouteMissingInspection
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.routing.inspection.TwigRouteMissingInspection
 */
class TwigRouteMissingInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()

        myFixture.copyFileToProject("TwigRouteMissingInspection.xml")
        myFixture.copyFileToProject("TwigRouteMissingInspection.php")
        myFixture.copyFileToProject("RouteDeprecatedInspection.php")
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/routing/inspection/fixtures"
    }

    fun testThatMissingRouteProvidesInspection() {
        assertLocalInspectionContains(
            "test.html.twig",
            "{{ path('fo<caret>obar') }}",
            "Symfony: Missing Route"
        )
    }

    fun testThatKnownRouteMustNotProvideErrorHighlight() {
        assertLocalInspectionNotContains(
            "test.html.twig",
            "{{ path('my_<caret>foobar') }}",
            "Symfony: Missing Route"
        )

        assertLocalInspectionNotContains(
            "test.html.twig",
            "{{ path('App\\\\Controller\\\\Foobar<caret>Controller') }}",
            "Symfony: Missing Route"
        )

        assertLocalInspectionNotContains(
            "test.html.twig",
            "{{ path('App\\\\Controller\\\\FooCon<caret>troller::foobar') }}",
            "Symfony: Missing Route"
        )
    }

    fun testThatInterpolatedStringMustBeIgnoredForInspection() {
        assertLocalInspectionNotContains(
            "test.html.twig",
            "{{ path('fo<caret>o#{langId}foobar') }}",
            "Symfony: Missing Route"
        )

        assertLocalInspectionNotContains(
            "test.html.twig",
            "{{ path('fo<caret>o#{segment.typeKey}foobar') }}",
            "Symfony: Missing Route"
        )
    }

    fun testRouteUsageForDeprecatedControllerActionProvidesInspection() {
        assertLocalInspectionContains(
            "test.html.twig",
            "{{ url('deprecated_<caret>route') }}",
            "Symfony: Controller action is deprecated"
        )

        assertLocalInspectionNotContains(
            "test.html.twig",
            "{{ url('active_<caret>route') }}",
            "Symfony: Controller action is deprecated"
        )
    }

    fun testSimilarRouteQuickFixReplacesTypo() {
        myFixture.enableInspections(TwigRouteMissingInspection::class.java)
        myFixture.configureByText(
            "test.html.twig",
            "{{ path('my_foob<caret>r') }}"
        )

        val intention = myFixture.findSingleIntention("Symfony: Apply Similar Suggestion")
        myFixture.launchAction(intention)

        myFixture.checkResult("{{ path('my_foobar') }}")
    }
}
