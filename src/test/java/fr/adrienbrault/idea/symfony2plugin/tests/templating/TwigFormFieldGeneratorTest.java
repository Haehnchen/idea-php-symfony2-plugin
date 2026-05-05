package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.twig.action.TwigFormFieldGenerator;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigFormFieldGenerator
 */
public class TwigFormFieldGeneratorTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testTwigFormFieldGeneratorIsAvailableForPrimitiveFormTypeFqnsFromControllerRender() {
        addFormControllerFixture();

        myFixture.addFileToProject("templates/form/generator.html.twig", "{{ form<caret> }}");
        myFixture.configureFromTempProjectFile("templates/form/generator.html.twig");

        assertTrue(myFixture.testAction(new TwigFormFieldGenerator()).isEnabledAndVisible());
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures";
    }

    private void addFormControllerFixture() {
        myFixture.copyFileToProject("ide-twig.json");
        myFixture.copyFileToProject("FormControllerTemplateVariables.php");
    }
}
