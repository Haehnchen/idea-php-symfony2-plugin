package fr.adrienbrault.idea.symfony2plugin.tests.templating

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.twig.action.TwigFormFieldGenerator

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigFormFieldGenerator
 */
class TwigFormFieldGeneratorTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testTwigFormFieldGeneratorIsAvailableForPrimitiveFormTypeFqnsFromControllerRender() {
        addFormControllerFixture()

        myFixture.addFileToProject("templates/form/generator.html.twig", "{{ form<caret> }}")
        myFixture.configureFromTempProjectFile("templates/form/generator.html.twig")

        assertTrue(myFixture.testAction(TwigFormFieldGenerator()).isEnabledAndVisible)
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures"
    }

    private fun addFormControllerFixture() {
        myFixture.copyFileToProject("ide-twig.json")
        myFixture.copyFileToProject("FormControllerTemplateVariables.php")
    }
}
