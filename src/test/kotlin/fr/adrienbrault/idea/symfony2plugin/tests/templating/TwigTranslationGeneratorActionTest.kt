package fr.adrienbrault.idea.symfony2plugin.tests.templating

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.twig.action.TwigTranslationGeneratorAction
import fr.adrienbrault.idea.symfony2plugin.twig.action.createTranslationSnippet

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigTranslationGeneratorAction
 */
class TwigTranslationGeneratorActionTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testActionAvailableForFileScope() {
        myFixture.configureByText("foo.html.twig", "{{ foo }}")

        assertTrue(myFixture.testAction(TwigTranslationGeneratorAction()).isEnabledAndVisible)
    }

    fun testCreateTranslationSnippetForDefaultDomain() {
        assertEquals(
            "{{ 'symfony_message'|trans }}",
            createTranslationSnippet("symfony_message", "messages", "messages")
        )
    }

    fun testCreateTranslationSnippetForExplicitDomain() {
        assertEquals(
            "{{ 'symfony.great'|trans({}, 'symfony') }}",
            createTranslationSnippet("symfony.great", "messages", "symfony")
        )
    }
}
