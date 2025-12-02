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

    public void testThatTokenParserWithTriggerDeprecationIsDetected() {
        assertLocalInspectionContains(
            "test.html.twig",
            "{% sand<caret>box %}",
            "Deprecated Twig tag"
        );

        assertLocalInspectionContains(
            "test.html.twig",
            "{% endsand<caret>box %}",
            "Deprecated Twig tag"
        );
    }

    public void testThatDeprecatedTwigFilterProvidesDeprecationWarning() {
        assertLocalInspectionContains(
            "test.html.twig",
            "{{ value|spaceless_deprecation_deprec<caret>ated }}",
            "Deprecated Twig filter"
        );

        assertLocalInspectionContains(
            "test.html.twig",
            "{{ value|spaceless_deprecation_i<caret>nfo }}",
            "Deprecated Twig filter"
        );

        // Test filter in apply block
        assertLocalInspectionContains(
            "test.html.twig",
            "{% apply spaceless_deprecation_deprec<caret>ated %}test{% endapply %}",
            "Deprecated Twig filter"
        );
    }

    public void testThatFilterWithTriggerDeprecationIsDetected() {
        assertLocalInspectionContains(
            "test.html.twig",
            "{{ value|filter_with_trigger_deprec<caret>ation }}",
            "Deprecated Twig filter"
        );

        // Test filter in apply block
        assertLocalInspectionContains(
            "test.html.twig",
            "{% apply filter_with_trigger_deprec<caret>ation %}test{% endapply %}",
            "Deprecated Twig filter"
        );
    }

    public void testThatDeprecatedTwigFunctionProvidesDeprecationWarning() {
        assertLocalInspectionContains(
            "test.html.twig",
            "{{ deprecated_fun<caret>ction() }}",
            "Deprecated Twig function"
        );

        assertLocalInspectionContains(
            "test.html.twig",
            "{{ deprecated_function_i<caret>nfo() }}",
            "Deprecated Twig function"
        );

        // Test function in if statement
        assertLocalInspectionContains(
            "test.html.twig",
            "{% if deprecated_fun<caret>ction() %}test{% endif %}",
            "Deprecated Twig function"
        );
    }

    public void testThatFunctionWithTriggerDeprecationIsDetected() {
        assertLocalInspectionContains(
            "test.html.twig",
            "{{ function_with_trigger_deprec<caret>ation() }}",
            "Deprecated Twig function"
        );

        // Test function in if statement
        assertLocalInspectionContains(
            "test.html.twig",
            "{% if function_with_trigger_deprec<caret>ation() %}test{% endif %}",
            "Deprecated Twig function"
        );
    }

    public void testThatNormalTwigFiltersAndFunctionsDoNotTriggerInspection() {
        assertLocalInspectionNotContains(
            "test.html.twig",
            "{{ value|tra<caret>ns }}",
            "Deprecated Twig tag"
        );

        assertLocalInspectionNotContains(
            "test.html.twig",
            "{{ value|trans_<caret>2 }}",
            "Deprecated Twig tag"
        );

        assertLocalInspectionNotContains(
            "test.html.twig",
            "{{ ma<caret>x() }}",
            "Deprecated Twig tag"
        );
    }
}
