package fr.adrienbrault.idea.symfony2plugin.tests.translation.inspection

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.translation.inspection.TwigTranslationDomainInspection

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigTranslationDomainInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()

        myFixture.copyFileToProject("symfony.de.yml", "Resources/translations/symfony.de.yml")
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/translation/inspection/fixtures"
    }

    fun testUnknownDomainIsInspected() {
        assertLocalInspectionContains(
            "f.html.twig",
            "{{ 'foo'|trans({}, 'UNK<caret>NOWN')) }}",
            TwigTranslationDomainInspection.MESSAGE
        )
    }

    fun testKnownDomainIsInspected() {
        assertLocalInspectionNotContains(
            "f.html.twig",
            "{{ 'foo'|trans({}, 'sy<caret>mfony')) }}",
            TwigTranslationDomainInspection.MESSAGE
        )
    }

    fun testUnknownDomainWithNamedArgumentColonSyntaxIsInspected() {
        assertLocalInspectionContains(
            "f.html.twig",
            "{{ 'foo'|trans(domain: 'UNK<caret>NOWN') }}",
            TwigTranslationDomainInspection.MESSAGE
        )
    }

    fun testKnownDomainWithNamedArgumentColonSyntaxIsNotInspected() {
        assertLocalInspectionNotContains(
            "f.html.twig",
            "{{ 'foo'|trans(domain: 'sy<caret>mfony') }}",
            TwigTranslationDomainInspection.MESSAGE
        )
    }

    fun testSimilarTranslationDomainQuickFixReplacesTypo() {
        myFixture.enableInspections(TwigTranslationDomainInspection::class.java)
        myFixture.configureByText(
            "f.html.twig",
            "{{ 'foo'|trans({}, 'sym<caret>fon')) }}"
        )

        val intention = myFixture.findSingleIntention("Symfony: Apply Similar Suggestion")
        myFixture.launchAction(intention)

        myFixture.checkResult("{{ 'foo'|trans({}, 'symfony')) }}")
    }
}
