package fr.adrienbrault.idea.symfony2plugin.tests.templating

import fr.adrienbrault.idea.symfony2plugin.templating.action.TwigExtendsGenerator
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigExtendsGenerator
 */
class TwigExtendsGeneratorTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testActionAvailableForTwigFile() {
        myFixture.configureByText("foo.html.twig", "<caret>")

        assertTrue(myFixture.testAction(TwigExtendsGenerator()).isEnabledAndVisible)
    }
}
