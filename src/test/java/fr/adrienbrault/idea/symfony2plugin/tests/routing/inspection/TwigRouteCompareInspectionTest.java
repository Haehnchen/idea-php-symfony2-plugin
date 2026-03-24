package fr.adrienbrault.idea.symfony2plugin.tests.routing.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.routing.inspection.TwigRouteCompareInspection
 */
public class TwigRouteCompareInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    private static final String INSPECTION_MESSAGE = "Symfony: Missing Route";

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("TwigRouteMissingInspection.xml");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/routing/inspection/fixtures";
    }

    public void testMissingRouteInEquality() {
        assertLocalInspectionContains("test.html.twig",
            "{% if app.request.attributes.get('_route') == 'unknow<caret>n_route' %}",
            INSPECTION_MESSAGE);

        assertLocalInspectionContains("test.html.twig",
            "{% if app.request.attributes.get('_route') != 'unknow<caret>n_route' %}",
            INSPECTION_MESSAGE);
    }

    public void testKnownRouteInEqualityHasNoInspection() {
        assertLocalInspectionNotContains("test.html.twig",
            "{% if app.request.attributes.get('_route') == 'my_<caret>foobar' %}",
            INSPECTION_MESSAGE);

        assertLocalInspectionNotContains("test.html.twig",
            "{% if app.request.attributes.get('_route') != 'my_<caret>foobar' %}",
            INSPECTION_MESSAGE);
    }

    public void testMissingRouteInSameAs() {
        assertLocalInspectionContains("test.html.twig",
            "{% if app.request.attributes.get('_route') is same as('unknow<caret>n_route') %}",
            INSPECTION_MESSAGE);
    }

    public void testKnownRouteInSameAsHasNoInspection() {
        assertLocalInspectionNotContains("test.html.twig",
            "{% if app.request.attributes.get('_route') is same as('my_<caret>foobar') %}",
            INSPECTION_MESSAGE);
    }

    public void testMissingRouteInArray() {
        assertLocalInspectionContains("test.html.twig",
            "{% if app.request.attributes.get('_route') in ['unknow<caret>n_route'] %}",
            INSPECTION_MESSAGE);
    }

    public void testKnownRouteInArrayHasNoInspection() {
        assertLocalInspectionNotContains("test.html.twig",
            "{% if app.request.attributes.get('_route') in ['my_<caret>foobar'] %}",
            INSPECTION_MESSAGE);
    }

    public void testStartsWithIsNotInspected() {
        // 'starts with' is a prefix match — not inspected for missing route
        assertLocalInspectionNotContains("test.html.twig",
            "{% if app.request.attributes.get('_route') starts with 'unknow<caret>n_route' %}",
            INSPECTION_MESSAGE);
    }

    public void testTernaryEqualitySyntax() {
        // {{ app.request.attributes.get('_route') == 'aaa' ? '' : '' }}
        assertLocalInspectionContains("test.html.twig",
            "{{ app.request.attributes.get('_route') == 'aa<caret>a' ? '' : '' }}",
            INSPECTION_MESSAGE);

        assertLocalInspectionNotContains("test.html.twig",
            "{{ app.request.attributes.get('_route') == 'my_<caret>foobar' ? '' : '' }}",
            INSPECTION_MESSAGE);
    }

    public void testTernaryDoesNotFlagEmptyStrings() {
        // The empty string branches of the ternary must not be flagged
        assertLocalInspectionNotContains("test.html.twig",
            "{{ app.request.attributes.get('_route') == 'my_foobar' ? '<caret>' : '' }}",
            INSPECTION_MESSAGE);

        assertLocalInspectionNotContains("test.html.twig",
            "{{ app.request.attributes.get('_route') == 'my_foobar' ? '' : '<caret>' }}",
            INSPECTION_MESSAGE);
    }
}
