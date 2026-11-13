package fr.adrienbrault.idea.symfony2plugin.tests.translation

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.translation.PhpTranslationParameterInspection
import fr.adrienbrault.idea.symfony2plugin.translation.inspection.TwigTranslationParameterInspection

class PhpTranslationParameterInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("HtmlTranslator.php")
        myFixture.copyFileToProject("PhpTranslationInspection.php")
        myFixture.addFileToProject(
            "translations/home+intl-icu.en.yaml",
            """
            project_aria_label: '{sum, number} contributions on {label}: {reviews, number} reviews, {prs, number} pull requests'
            invitation: '{gender, select, female {She} male {He} other {They}} invited {guest}'
            broken: 'Hello {name'
            """.trimIndent()
        )
        myFixture.addFileToProject("translations/home+intl-icu.fr.yaml", "invitation: '{guest} est invité chez {host}'")
        myFixture.addFileToProject("translations/legacy.en.yaml", "greeting: 'Hello %name%'")
    }

    override fun getTestDataPath(): String = "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/translation/fixtures"

    fun testUnknownParameterOfTranslatorTransIsReported() {
        assertUnknown("\$x->trans('project_aria_label', ['su<caret>mm' => 1], 'home');")
        assertUnknown("\$x->trans(id: 'project_aria_label', domain: 'home', parameters: ['fo<caret>o' => 1]);")
        assertUnknown("\$x->transChoice('project_aria_label', 1, ['fo<caret>o' => 1], 'home');")
    }

    fun testUnknownParameterOfTranslatableMessageIsReported() {
        assertUnknown("new \\Symfony\\Component\\Translation\\TranslatableMessage('project_aria_label', ['fo<caret>o' => 1], 'home');")
        assertUnknown("\\Symfony\\Component\\Translation\\t('project_aria_label', ['fo<caret>o' => 1], 'home');")
    }

    fun testUnknownParameterOfHtmlTranslatorIsReported() {
        assertUnknown("/** @var \\Symfony\\Component\\Translation\\HtmlTranslator \$translator */ \$translator->trans('invitation', [], ['fo<caret>o' => 1], 'home');")
        assertKnown("/** @var \\Symfony\\Component\\Translation\\HtmlTranslator \$translator */ \$translator->trans('invitation', ['fo<caret>o' => []], [], 'home');")
    }

    fun testKnownParametersAreNotReported() {
        assertKnown("\$x->trans('project_aria_label', ['su<caret>m' => 1], 'home');")
        assertKnown("\$x->trans('project_aria_label', ['%su<caret>m%' => 1], 'home');")
        assertKnown("\$x->trans('project_aria_label', ['{su<caret>m}' => 1], 'home');")
        assertKnown("\$x->trans('invitation', ['ho<caret>st' => 1], 'home');")
    }

    fun testParametersAreNotReportedWhenTheMessageCannotBeChecked() {
        assertKnown("\$x->trans('greeting', ['fo<caret>o' => 1], 'legacy');")
        assertKnown("\$x->trans('broken', ['fo<caret>o' => 1], 'home');")
        assertKnown("\$x->trans('missing', ['fo<caret>o' => 1], 'home');")
        assertKnown("\$x->trans('project_aria_label', ['fo<caret>o' => 1], \$domain);")
    }

    private fun assertUnknown(code: String) {
        assertLocalInspectionContains(PhpTranslationParameterInspection::class.java, "test.php", PREFIX + code, TwigTranslationParameterInspection.MESSAGE)
    }

    private fun assertKnown(code: String) {
        assertLocalInspectionNotContains(PhpTranslationParameterInspection::class.java, "test.php", PREFIX + code, TwigTranslationParameterInspection.MESSAGE)
    }

    private companion object {
        const val PREFIX = "<?php\n/** @var \$x \\Symfony\\Component\\Translation\\TranslatorInterface */\n"
    }
}
