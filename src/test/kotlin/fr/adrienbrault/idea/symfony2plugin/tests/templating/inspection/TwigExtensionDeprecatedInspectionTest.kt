package fr.adrienbrault.idea.symfony2plugin.tests.templating.inspection

import fr.adrienbrault.idea.symfony2plugin.templating.inspection.TwigExtensionDeprecatedInspection
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.inspection.TwigExtensionDeprecatedInspection
 */
class TwigExtensionDeprecatedInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("TwigExtensionDeprecatedInspection.php")
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/templating/inspection/fixtures"
    }

    fun testThatDeprecatedTwigTokenProvidesDeprecatedMessageFromPhpClass() {
        assertLocalInspectionContains(TwigExtensionDeprecatedInspection::class.java,
            "test.html.twig",
            "{% spac<caret>eless % }",
            "Deprecated: Foobar deprecated message"
        )

        assertLocalInspectionContains(TwigExtensionDeprecatedInspection::class.java,
            "test.html.twig",
            "{% endspac<caret>eless % }",
            "Deprecated: Foobar deprecated message"
        )
    }

    fun testThatAttributeDeprecatedTwigTokenIsDetected() {
        assertLocalInspectionContains(TwigExtensionDeprecatedInspection::class.java,
            "test.html.twig",
            "{% attribute_depre<caret>cated %}",
            "Deprecated Twig tag"
        )
    }

    fun testThatTokenParserWithTriggerDeprecationIsDetected() {
        assertLocalInspectionContains(TwigExtensionDeprecatedInspection::class.java,
            "test.html.twig",
            "{% sand<caret>box %}",
            "Deprecated Twig tag"
        )

        assertLocalInspectionContains(TwigExtensionDeprecatedInspection::class.java,
            "test.html.twig",
            "{% endsand<caret>box %}",
            "Deprecated Twig tag"
        )
    }

    fun testThatDeprecatedTwigFilterProvidesDeprecationWarning() {
        assertLocalInspectionContains(TwigExtensionDeprecatedInspection::class.java,
            "test.html.twig",
            "{{ value|spaceless_deprecation_deprec<caret>ated }}",
            "Deprecated Twig filter"
        )

        assertLocalInspectionContains(TwigExtensionDeprecatedInspection::class.java,
            "test.html.twig",
            "{{ value|spaceless_deprecation_i<caret>nfo }}",
            "Deprecated Twig filter"
        )

        // Test filter in apply block
        assertLocalInspectionContains(TwigExtensionDeprecatedInspection::class.java,
            "test.html.twig",
            "{% apply spaceless_deprecation_deprec<caret>ated %}test{% endapply %}",
            "Deprecated Twig filter"
        )
    }

    fun testThatFilterWithTriggerDeprecationIsDetected() {
        assertLocalInspectionContains(TwigExtensionDeprecatedInspection::class.java,
            "test.html.twig",
            "{{ value|filter_with_trigger_deprec<caret>ation }}",
            "Deprecated Twig filter"
        )

        // Test filter in apply block
        assertLocalInspectionContains(TwigExtensionDeprecatedInspection::class.java,
            "test.html.twig",
            "{% apply filter_with_trigger_deprec<caret>ation %}test{% endapply %}",
            "Deprecated Twig filter"
        )
    }

    fun testThatDeprecatedTwigFunctionProvidesDeprecationWarning() {
        assertLocalInspectionContains(TwigExtensionDeprecatedInspection::class.java,
            "test.html.twig",
            "{{ deprecated_fun<caret>ction() }}",
            "Deprecated Twig function"
        )

        assertLocalInspectionContains(TwigExtensionDeprecatedInspection::class.java,
            "test.html.twig",
            "{{ deprecated_function_i<caret>nfo() }}",
            "Deprecated Twig function"
        )

        // Test function in if statement
        assertLocalInspectionContains(TwigExtensionDeprecatedInspection::class.java,
            "test.html.twig",
            "{% if deprecated_fun<caret>ction() %}test{% endif %}",
            "Deprecated Twig function"
        )
    }

    fun testThatFunctionWithTriggerDeprecationIsDetected() {
        assertLocalInspectionContains(TwigExtensionDeprecatedInspection::class.java,
            "test.html.twig",
            "{{ function_with_trigger_deprec<caret>ation() }}",
            "Deprecated Twig function"
        )

        // Test function in if statement
        assertLocalInspectionContains(TwigExtensionDeprecatedInspection::class.java,
            "test.html.twig",
            "{% if function_with_trigger_deprec<caret>ation() %}test{% endif %}",
            "Deprecated Twig function"
        )
    }

    fun testThatNormalTwigFiltersAndFunctionsDoNotTriggerInspection() {
        assertLocalInspectionNotContains(TwigExtensionDeprecatedInspection::class.java,
            "test.html.twig",
            "{{ value|tra<caret>ns }}",
            "Deprecated Twig tag"
        )

        assertLocalInspectionNotContains(TwigExtensionDeprecatedInspection::class.java,
            "test.html.twig",
            "{{ value|trans_<caret>2 }}",
            "Deprecated Twig tag"
        )

        assertLocalInspectionNotContains(TwigExtensionDeprecatedInspection::class.java,
            "test.html.twig",
            "{{ ma<caret>x() }}",
            "Deprecated Twig tag"
        )
    }
}
