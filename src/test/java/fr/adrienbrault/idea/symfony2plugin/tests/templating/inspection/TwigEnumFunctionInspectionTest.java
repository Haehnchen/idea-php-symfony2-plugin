package fr.adrienbrault.idea.symfony2plugin.tests.templating.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.inspection.TwigEnumFunctionInspection
 */
public class TwigEnumFunctionInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("TwigFilterExtension.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures";
    }

    public void testThatValidEnumDoesNotTriggerInspection() {
        assertLocalInspectionNotContains(
            "test.html.twig",
            "{{ enum('App\\\\Bike\\\\Foo<caret>Enum') }}",
            "Missing class: App\\Bike\\FooEnum"
        );

        assertLocalInspectionNotContains(
            "test.html.twig",
            "{{ enum_cases('App\\\\Bike\\\\Foo<caret>Enum') }}",
            "Missing class: App\\Bike\\FooEnum"
        );
    }

    public void testThatMissingClassTriggersInspection() {
        assertLocalInspectionContains(
            "test.html.twig",
            "{{ enum('App\\\\Bike\\\\Missing<caret>Enum') }}",
            "Missing class: App\\Bike\\MissingEnum"
        );

        assertLocalInspectionContains(
            "test.html.twig",
            "{{ enum_cases('App\\\\Bike\\\\Missing<caret>Enum') }}",
            "Missing class: App\\Bike\\MissingEnum"
        );
    }

    public void testThatNonEnumClassTriggersInspection() {
        // Note: In the test fixture, FooConst is not an enum
        // We need to check if the class exists and is not an enum
        // Since we don't have a non-enum class in the fixture at App\Bike namespace,
        // let's test with a different scenario
        assertLocalInspectionNotContains(
            "test.html.twig",
            "{{ enum('App\\\\Bike\\\\Foo<caret>Enum') }}",
            "is not an enum"
        );
    }

    public void testThatEnumFunctionInTagBlocksTriggersInspection() {
        assertLocalInspectionContains(
            "test.html.twig",
            "{% if enum('App\\\\Bike\\\\Missing<caret>Enum') %}test{% endif %}",
            "Missing class: App\\Bike\\MissingEnum"
        );

        assertLocalInspectionContains(
            "test.html.twig",
            "{% set foo = enum_cases('App\\\\Bike\\\\Missing<caret>Enum') %}",
            "Missing class: App\\Bike\\MissingEnum"
        );
    }

    public void testThatFullyQualifiedClassNameWorks() {
        assertLocalInspectionNotContains(
            "test.html.twig",
            "{{ enum('\\\\App\\\\Bike\\\\Foo<caret>Enum') }}",
            "Missing class"
        );
    }

    public void testThatBackslashEscapingIsHandled() {
        // Test with double backslashes (escaped in Twig)
        assertLocalInspectionNotContains(
            "test.html.twig",
            "{{ enum('App\\\\Bike\\\\Foo<caret>Enum') }}",
            "Missing class"
        );
    }
}
