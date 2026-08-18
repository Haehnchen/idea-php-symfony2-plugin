package fr.adrienbrault.idea.symfony2plugin.tests.translation.inspection

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.translation.PhpTranslationKeyInspection
import fr.adrienbrault.idea.symfony2plugin.translation.inspection.TwigTranslationKeyInspection

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.translation.inspection.TwigTranslationKeyInspection
 */
class TwigTranslationKeyInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
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
            "{{ 'f<caret>oo'|trans({}, 'symfony')) }}",
            PhpTranslationKeyInspection.MESSAGE
        )

        assertLocalInspectionContains(
            "f.html.twig",
            "{% trans_default_domain symfony %}\n{{ 'f<caret>oo'|trans }}",
            PhpTranslationKeyInspection.MESSAGE
        )
    }

    fun testKnownKeyWithNamedArgumentColonSyntaxIsNotInspected() {
        assertLocalInspectionNotContains(
            "f.html.twig",
            "{{ 'symfon<caret>y.great'|trans(domain: 'symfony') }}",
            PhpTranslationKeyInspection.MESSAGE
        )
    }

    fun testThatInterpolatedStringsMustNotInspected() {
        assertLocalInspectionNotContains(
            "f.html.twig",
            "{{ 'ti<caret>tle.#{word}'|trans({}, 'symfony')) }}",
            PhpTranslationKeyInspection.MESSAGE
        )
    }

    fun testKnownDomainIsInspected() {
        assertLocalInspectionNotContains(
            "f.html.twig",
            "{{ 'symfon<caret>y.great'|trans({}, 'symfony')) }}",
            PhpTranslationKeyInspection.MESSAGE
        )

        assertLocalInspectionNotContains(
            "f.html.twig",
            "{% trans_default_domain symfony %}\n{{ 'symfon<caret>y.great'|trans }}",
            PhpTranslationKeyInspection.MESSAGE
        )
    }

    fun testSimilarTranslationKeyQuickFixReplacesTypo() {
        myFixture.enableInspections(TwigTranslationKeyInspection::class.java)
        myFixture.configureByText(
            "f.html.twig",
            "{{ 'symfon<caret>y.grea'|trans({}, 'symfony')) }}"
        )

        val intention = myFixture.findSingleIntention("Symfony: Apply Similar Suggestion")
        myFixture.launchAction(intention)

        myFixture.checkResult("{{ 'symfony.great'|trans({}, 'symfony')) }}")
    }
}
