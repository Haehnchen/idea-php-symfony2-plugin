package fr.adrienbrault.idea.symfonyplugin.tests.templating;

import fr.adrienbrault.idea.symfonyplugin.twig.action.TwigTranslationGeneratorAction;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigTranslationGeneratorAction
 */
public class TwigTranslationGeneratorActionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testActionAvailableForFileScope() {
        myFixture.configureByText("foo.html.twig", "{{ foo }}");

        assertTrue(myFixture.testAction(new TwigTranslationGeneratorAction()).isEnabledAndVisible());
    }
}
