package fr.adrienbrault.idea.symfony2plugin.tests.translation.intention

import com.jetbrains.twig.TwigFileType
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.translation.intention.TwigTranslationKeyIntention
 */
class TwigTranslationKeyIntentionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()

        myFixture.copyFileToProject("symfony.de.yml", "Resources/translations/symfony.de.yml")
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/translation/intention/fixtures"
    }

    fun testThatKeyAlreadyExistsAndProvidesIntentionForOtherDomains() {
        assertIntentionIsAvailable(
            TwigFileType.INSTANCE,
            "{{ 'symfo<caret>ny.great'|trans({}, 'symfony')) }}",
            "Symfony: create translation key"
        )
    }
}
