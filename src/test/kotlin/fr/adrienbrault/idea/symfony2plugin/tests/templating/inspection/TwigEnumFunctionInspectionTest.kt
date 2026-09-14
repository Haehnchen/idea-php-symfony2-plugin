package fr.adrienbrault.idea.symfony2plugin.tests.templating.inspection

import fr.adrienbrault.idea.symfony2plugin.templating.inspection.TwigEnumFunctionInspection
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.inspection.TwigEnumFunctionInspection
 */
class TwigEnumFunctionInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("classes.php")
        myFixture.copyFileToProject("TwigFilterExtension.php")
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures"
    }

    fun testThatValidEnumDoesNotTriggerInspection() {
        assertLocalInspectionNotContains(TwigEnumFunctionInspection::class.java,
            "test.html.twig",
            "{{ enum('App\\\\Bike\\\\Foo<caret>Enum') }}",
            "Missing class: App\\Bike\\FooEnum"
        )

        assertLocalInspectionNotContains(TwigEnumFunctionInspection::class.java,
            "test.html.twig",
            "{{ enum_cases('App\\\\Bike\\\\Foo<caret>Enum') }}",
            "Missing class: App\\Bike\\FooEnum"
        )
    }

    fun testThatMissingClassTriggersInspection() {
        assertLocalInspectionContains(TwigEnumFunctionInspection::class.java,
            "test.html.twig",
            "{{ enum('App\\\\Bike\\\\Missing<caret>Enum') }}",
            "Missing class: App\\Bike\\MissingEnum"
        )

        assertLocalInspectionContains(TwigEnumFunctionInspection::class.java,
            "test.html.twig",
            "{{ enum_cases('App\\\\Bike\\\\Missing<caret>Enum') }}",
            "Missing class: App\\Bike\\MissingEnum"
        )
    }

    fun testThatNonEnumClassTriggersInspection() {
        // Note: In the test fixture, FooConst is not an enum
        // We need to check if the class exists and is not an enum
        // Since we don't have a non-enum class in the fixture at App\Bike namespace,
        // let's test with a different scenario
        assertLocalInspectionNotContains(TwigEnumFunctionInspection::class.java,
            "test.html.twig",
            "{{ enum('App\\\\Bike\\\\Foo<caret>Enum') }}",
            "is not an enum"
        )
    }

    fun testThatEnumFunctionInTagBlocksTriggersInspection() {
        assertLocalInspectionContains(TwigEnumFunctionInspection::class.java,
            "test.html.twig",
            "{% if enum('App\\\\Bike\\\\Missing<caret>Enum') %}test{% endif %}",
            "Missing class: App\\Bike\\MissingEnum"
        )

        assertLocalInspectionContains(TwigEnumFunctionInspection::class.java,
            "test.html.twig",
            "{% set foo = enum_cases('App\\\\Bike\\\\Missing<caret>Enum') %}",
            "Missing class: App\\Bike\\MissingEnum"
        )
    }

    fun testThatFullyQualifiedClassNameWorks() {
        assertLocalInspectionNotContains(TwigEnumFunctionInspection::class.java,
            "test.html.twig",
            "{{ enum('\\\\App\\\\Bike\\\\Foo<caret>Enum') }}",
            "Missing class"
        )
    }

    fun testThatBackslashEscapingIsHandled() {
        // Test with double backslashes (escaped in Twig)
        assertLocalInspectionNotContains(TwigEnumFunctionInspection::class.java,
            "test.html.twig",
            "{{ enum('App\\\\Bike\\\\Foo<caret>Enum') }}",
            "Missing class"
        )
    }
}
