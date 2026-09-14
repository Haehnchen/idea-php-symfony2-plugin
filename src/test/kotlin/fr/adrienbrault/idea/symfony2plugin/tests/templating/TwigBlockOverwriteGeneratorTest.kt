package fr.adrienbrault.idea.symfony2plugin.tests.templating

import fr.adrienbrault.idea.symfony2plugin.templating.action.TwigBlockOverwriteGenerator
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigBlockOverwriteGenerator
 */
class TwigBlockOverwriteGeneratorTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testActionAvailableForTwigFile() {
        myFixture.configureByText("foo.html.twig", "<caret>")

        assertTrue(myFixture.testAction(TwigBlockOverwriteGenerator()).isEnabledAndVisible)
    }
}
