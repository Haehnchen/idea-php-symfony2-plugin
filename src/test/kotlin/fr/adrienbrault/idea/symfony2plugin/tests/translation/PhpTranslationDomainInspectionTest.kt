package fr.adrienbrault.idea.symfony2plugin.tests.translation

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.translation.PhpTranslationDomainInspection

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see PhpTranslationDomainInspection
 */
class PhpTranslationDomainInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("PhpTranslationInspection.php")
        myFixture.copyFileToProject("symfony.de.yml", "Resources/translations/symfony.de.yml")
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/translation/fixtures"
    }

    fun testThatPhpTranslationDomainInspectionsAreProvided() {
        assertLocalInspectionContains(
            "test.php", "<?php\n" +
                "/** @var \$x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "\$x->trans('foobar', [], 'dom<caret>ain')",
            PhpTranslationDomainInspection.MESSAGE
        )

        assertLocalInspectionContains(
            "test.php", "<?php\n" +
                "/** @var \$x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "\$x->transChoice('foobar', 1, [], 'do<caret>main')",
            PhpTranslationDomainInspection.MESSAGE
        )

        assertLocalInspectionNotContains(
            "test.php", "<?php\n" +
                "/** @var \$x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "\$x->trans('foobar', [], 'sym<caret>fony')",
            PhpTranslationDomainInspection.MESSAGE
        )

        assertLocalInspectionNotContains(
            "test.php", "<?php\n" +
                "/** @var \$x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "\$x->transChoice('foobar', 1, [], 'sym<caret>fony')",
            PhpTranslationDomainInspection.MESSAGE
        )

        assertLocalInspectionNotContains(
            "test.php", "<?php\n" +
                "/** @var \$x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "\$x->trans('foo<caret>bar')",
            PhpTranslationDomainInspection.MESSAGE
        )

        assertLocalInspectionContains(
            "test.php", "<?php\n" +
                "/** @var \$x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "\$x->trans('id', domain: 'dom<caret>ain')",
            PhpTranslationDomainInspection.MESSAGE
        )
    }

    fun testThatPhpTranslationDomainInspectionsForTranslatableMessageAreProvided() {
        assertLocalInspectionContains(
            "test.php", "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage('foobar', [], 'do<caret>main');",
            PhpTranslationDomainInspection.MESSAGE
        )

        assertLocalInspectionContains(
            "test.php", "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage('foobar', [], 'foo<caret>bar');",
            PhpTranslationDomainInspection.MESSAGE
        )

        assertLocalInspectionNotContains(
            "test.php", "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage('foobar', [], 'sym<caret>fony');",
            PhpTranslationDomainInspection.MESSAGE
        )

        assertLocalInspectionNotContains(
            "test.php", "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage(domain: 'sym<caret>fony');",
            PhpTranslationDomainInspection.MESSAGE
        )
    }

    fun testThatPhpTranslationDomainInspectionsForTranslatableMessageViaTFunctionAreProvided() {
        assertLocalInspectionContains(
            "test.php", "<?php\n" +
                "use function Symfony\\Component\\Translation\\t;\n" +
                "t('foobar', [], 'do<caret>main');",
            PhpTranslationDomainInspection.MESSAGE
        )

        assertLocalInspectionContains(
            "test.php", "<?php\n" +
                "use function Symfony\\Component\\Translation\\t;\n" +
                "t('foobar', [], 'foo<caret>bar');",
            PhpTranslationDomainInspection.MESSAGE
        )

        assertLocalInspectionNotContains(
            "test.php", "<?php\n" +
                "use function Symfony\\Component\\Translation\\t;\n" +
                "t('foobar', [], 'sym<caret>fony');",
            PhpTranslationDomainInspection.MESSAGE
        )

        assertLocalInspectionNotContains(
            "test.php", "<?php\n" +
                "use function Symfony\\Component\\Translation\\t;\n" +
                "t(domain: 'sym<caret>fony');",
            PhpTranslationDomainInspection.MESSAGE
        )
    }
}
