package fr.adrienbrault.idea.symfony2plugin.tests.routing.inspection

import fr.adrienbrault.idea.symfony2plugin.routing.inspection.TwigRouteCompareInspection
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.routing.inspection.TwigRouteCompareInspection
 */
class TwigRouteCompareInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    companion object {
        private const val INSPECTION_MESSAGE = "Symfony: Missing Route"
    }

    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("TwigRouteMissingInspection.xml")
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/routing/inspection/fixtures"
    }

    fun testMissingRouteInEquality() {
        assertLocalInspectionContains(TwigRouteCompareInspection::class.java, "test.html.twig",
            "{% if app.request.attributes.get('_route') == 'unknow<caret>n_route' %}",
            INSPECTION_MESSAGE)

        assertLocalInspectionContains(TwigRouteCompareInspection::class.java, "test.html.twig",
            "{% if app.request.attributes.get('_route') != 'unknow<caret>n_route' %}",
            INSPECTION_MESSAGE)
    }

    fun testKnownRouteInEqualityHasNoInspection() {
        assertLocalInspectionNotContains(TwigRouteCompareInspection::class.java, "test.html.twig",
            "{% if app.request.attributes.get('_route') == 'my_<caret>foobar' %}",
            INSPECTION_MESSAGE)

        assertLocalInspectionNotContains(TwigRouteCompareInspection::class.java, "test.html.twig",
            "{% if app.request.attributes.get('_route') != 'my_<caret>foobar' %}",
            INSPECTION_MESSAGE)
    }

    fun testMissingRouteInSameAs() {
        assertLocalInspectionContains(TwigRouteCompareInspection::class.java, "test.html.twig",
            "{% if app.request.attributes.get('_route') is same as('unknow<caret>n_route') %}",
            INSPECTION_MESSAGE)
    }

    fun testKnownRouteInSameAsHasNoInspection() {
        assertLocalInspectionNotContains(TwigRouteCompareInspection::class.java, "test.html.twig",
            "{% if app.request.attributes.get('_route') is same as('my_<caret>foobar') %}",
            INSPECTION_MESSAGE)
    }

    fun testMissingRouteInArray() {
        assertLocalInspectionContains(TwigRouteCompareInspection::class.java, "test.html.twig",
            "{% if app.request.attributes.get('_route') in ['unknow<caret>n_route'] %}",
            INSPECTION_MESSAGE)
    }

    fun testKnownRouteInArrayHasNoInspection() {
        assertLocalInspectionNotContains(TwigRouteCompareInspection::class.java, "test.html.twig",
            "{% if app.request.attributes.get('_route') in ['my_<caret>foobar'] %}",
            INSPECTION_MESSAGE)
    }

    fun testStartsWithIsNotInspected() {
        // 'starts with' is a prefix match — not inspected for missing route
        assertLocalInspectionNotContains(TwigRouteCompareInspection::class.java, "test.html.twig",
            "{% if app.request.attributes.get('_route') starts with 'unknow<caret>n_route' %}",
            INSPECTION_MESSAGE)
    }

    fun testTernaryEqualitySyntax() {
        // {{ app.request.attributes.get('_route') == 'aaa' ? '' : '' }}
        assertLocalInspectionContains(TwigRouteCompareInspection::class.java, "test.html.twig",
            "{{ app.request.attributes.get('_route') == 'aa<caret>a' ? '' : '' }}",
            INSPECTION_MESSAGE)

        assertLocalInspectionNotContains(TwigRouteCompareInspection::class.java, "test.html.twig",
            "{{ app.request.attributes.get('_route') == 'my_<caret>foobar' ? '' : '' }}",
            INSPECTION_MESSAGE)
    }

    fun testTernaryDoesNotFlagEmptyStrings() {
        // The empty string branches of the ternary must not be flagged
        assertLocalInspectionNotContains(TwigRouteCompareInspection::class.java, "test.html.twig",
            "{{ app.request.attributes.get('_route') == 'my_foobar' ? '<caret>' : '' }}",
            INSPECTION_MESSAGE)

        assertLocalInspectionNotContains(TwigRouteCompareInspection::class.java, "test.html.twig",
            "{{ app.request.attributes.get('_route') == 'my_foobar' ? '' : '<caret>' }}",
            INSPECTION_MESSAGE)
    }
}
