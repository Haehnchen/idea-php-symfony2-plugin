package fr.adrienbrault.idea.symfony2plugin.tests.templating.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.inspection.TwigExtensionDeprecatedInspection
 */
public class TwigExtensionDeprecatedInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("TwigExtensionDeprecatedInspection.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/inspection/fixtures";
    }

    public void testThatDeprecatedTwigTokenProvidesDeprecatedMessageFromPhpClass() {
        assertLocalInspectionContains(
            "test.html.twig",
                "{% spac<caret>eless % }",
            "Deprecated: Foobar deprecated message"
        );

        assertLocalInspectionContains(
            "test.html.twig",
            "{% endspac<caret>eless % }",
            "Deprecated: Foobar deprecated message"
        );
    }
}
