package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import fr.adrienbrault.idea.symfony2plugin.templating.TwigTranslationGeneratorAction;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigTranslationGeneratorAction
 */
public class TwigTranslationGeneratorActionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testActionAvailableForFileScope() {
        myFixture.configureByText("foo.html.twig", "{{ foo }}");

        assertTrue(myFixture.testAction(new TwigTranslationGeneratorAction()).isEnabledAndVisible());
    }
}
