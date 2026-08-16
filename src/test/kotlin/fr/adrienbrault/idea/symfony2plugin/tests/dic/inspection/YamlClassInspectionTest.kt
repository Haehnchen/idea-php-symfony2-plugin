package fr.adrienbrault.idea.symfony2plugin.tests.dic.inspection

import fr.adrienbrault.idea.symfony2plugin.dic.inspection.YamlClassInspection
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.inspection.YamlClassInspection
 */
class YamlClassInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()

        myFixture.copyFileToProject("YamlClassInspection.php")
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/dic/inspection/fixtures"
    }

    fun testInspectionForClass() {
        assertLocalInspectionContains("services.yml", "services:\n  class: Args\\Fo<caret>oBar", YamlClassInspection.MESSAGE_MISSING_CLASS)
        assertLocalInspectionContains("services.yml", "services:\n  class: 'Args\\Fo<caret>oBar'", YamlClassInspection.MESSAGE_MISSING_CLASS)
        assertLocalInspectionContains("services.yml", "services:\n  class: \"Args\\Fo<caret>oBar\"", YamlClassInspection.MESSAGE_MISSING_CLASS)
        assertLocalInspectionContains("services.yml", "services:\n  factory_class: Args\\Fo<caret>oBar", YamlClassInspection.MESSAGE_MISSING_CLASS)
        assertLocalInspectionNotContains("services.yml", "services:\n  factory_class: Args\\Fo<caret>o", YamlClassInspection.MESSAGE_MISSING_CLASS)

        assertLocalInspectionContains("services.yml", "parameters:\n  foo.class: Args\\Fo<caret>oBar", YamlClassInspection.MESSAGE_MISSING_CLASS)
        assertLocalInspectionContains("services.yml", "parameters:\n  foo.class: 'Args\\Fo<caret>oBar'", YamlClassInspection.MESSAGE_MISSING_CLASS)
        assertLocalInspectionContains("services.yml", "parameters:\n  foo.class: \"Args\\Fo<caret>oBar\"", YamlClassInspection.MESSAGE_MISSING_CLASS)
        assertLocalInspectionNotContains("services.yml", "parameters:\n  foo.class: Args\\Fo<caret>o", YamlClassInspection.MESSAGE_MISSING_CLASS)

        assertLocalInspectionContains("services.yml", "services:\n  class: Args\\Fo<caret>O", YamlClassInspection.MESSAGE_WRONG_CASING)
        assertLocalInspectionContains("services.yml", "services:\n  class: 'Args\\Fo<caret>O'", YamlClassInspection.MESSAGE_WRONG_CASING)
        assertLocalInspectionContains("services.yml", "services:\n  class: \"Args\\Fo<caret>O\"", YamlClassInspection.MESSAGE_WRONG_CASING)
        assertLocalInspectionContains("services.yml", "services:\n  factory_class: Args\\Fo<caret>O", YamlClassInspection.MESSAGE_WRONG_CASING)
        assertLocalInspectionNotContains("services.yml", "services:\n  factory_class: Args\\Fo<caret>o", YamlClassInspection.MESSAGE_WRONG_CASING)

        assertLocalInspectionContains("services.yml", "parameters:\n  foo.class: Args\\Fo<caret>O", YamlClassInspection.MESSAGE_WRONG_CASING)
        assertLocalInspectionContains("services.yml", "parameters:\n  foo.class: 'Args\\Fo<caret>O'", YamlClassInspection.MESSAGE_WRONG_CASING)
        assertLocalInspectionContains("services.yml", "parameters:\n  foo.class: \"Args\\Fo<caret>O\"", YamlClassInspection.MESSAGE_WRONG_CASING)
        assertLocalInspectionNotContains("services.yml", "parameters:\n  foo.class: Args\\Fo<caret>o", YamlClassInspection.MESSAGE_WRONG_CASING)

        assertLocalInspectionContains("services.yml", "services:\n  Args\\Fo<caret>oBar: ~", YamlClassInspection.MESSAGE_MISSING_CLASS)
        assertLocalInspectionNotContains("services.yml", "services:\n  foo.class: Args\\Fo<caret>o", YamlClassInspection.MESSAGE_WRONG_CASING)

        assertLocalInspectionNotContains(
                "services.yml",
                "services:\n" +
                        "  Args\\Fo<caret>oBar:\n" +
                        "       resource: ~",
                YamlClassInspection.MESSAGE_MISSING_CLASS
        )

        assertLocalInspectionNotContains(
                "services.yml",
                "services:\n" +
                        "  Args\\Fo<caret>oBar:\n" +
                        "       exclude: ~",
                YamlClassInspection.MESSAGE_MISSING_CLASS
        )
    }
}
