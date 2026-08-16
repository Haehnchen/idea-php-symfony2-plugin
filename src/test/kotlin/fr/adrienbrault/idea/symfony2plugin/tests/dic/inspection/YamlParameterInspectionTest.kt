package fr.adrienbrault.idea.symfony2plugin.tests.dic.inspection

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.inspection.YamlParameterInspection
 */
class YamlParameterInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()

        myFixture.copyFileToProject("YamlParameterInspection.xml")
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/dic/inspection/fixtures"
    }

    fun testParameterInspection() {
        assertLocalInspectionContains("services.yml", "services:\n   %foo_<caret>missing%", "Symfony: Missing Parameter")
        assertLocalInspectionNotContains("services.yml", "services:\n   %foo_p<caret>arameter%", "Symfony: Missing Parameter")

        assertLocalInspectionContains("services.yml", "services:\n   %Foo_<caret>missing%", "Symfony: Missing Parameter")
        assertLocalInspectionNotContains("services.yml", "services:\n   %Foo_p<caret>arameter%", "Symfony: Missing Parameter")

        assertLocalInspectionContains("services.yml", "services:\n   [ '%Foo_<caret>missing%' ]\n", "Symfony: Missing Parameter")
        assertLocalInspectionContains("services.yml", "services:\n   [ \"%Foo_<caret>missing%\" ]\n", "Symfony: Missing Parameter")
        assertLocalInspectionNotContains("services.yml", "services:\n   %kernel.root_dir%/../we<caret>b/%webpath_modelmasks%", "Symfony: Missing Parameter")
    }

    fun testParameterInspectionForEnvMustNotAnnotationAsMissing() {
        assertLocalInspectionNotContains("services.yml", "services:\n   %env(FO<caret>O)%", "Symfony: Missing Parameter")
        assertLocalInspectionNotContains("services.yml", "services:\n   '%env(FO<caret>O)%'", "Symfony: Missing Parameter")
        assertLocalInspectionNotContains("services.yml", "services:\n   \"%env(FO<caret>O)%\"", "Symfony: Missing Parameter")
        assertLocalInspectionNotContains("services.yml", "services:\n   '%ENV(FO<caret>O)%'", "Symfony: Missing Parameter")
    }
}
