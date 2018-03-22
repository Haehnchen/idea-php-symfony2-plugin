package fr.adrienbrault.idea.symfony2plugin.tests.routing.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.routing.inspection.TwigRouteMissingInspection
 */
public class TwigRouteMissingInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("TwigRouteMissingInspection.xml");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/routing/inspection/fixtures";
    }

    public void testThatMissingRouteProvidesInspection() {
        assertLocalInspectionContains(
            "test.html.twig",
            "{{ path('fo<caret>obar') }}",
            "Missing Route"
        );
    }

    public void testThatKnownRouteMustNotProvideErrorHighlight() {
        assertLocalInspectionNotContains(
            "test.html.twig",
            "{{ path('my_<caret>foobar') }}",
            "Missing Route"
        );
    }

    public void testThatInterpolatedStringMustBeIgnoredForInspection() {
        assertLocalInspectionNotContains(
            "test.html.twig",
            "{{ path('fo<caret>o#{langId}foobar') }}",
            "Missing Route"
        );

        assertLocalInspectionNotContains(
            "test.html.twig",
            "{{ path('fo<caret>o#{segment.typeKey}foobar') }}",
            "Missing Route"
        );


    }
}
