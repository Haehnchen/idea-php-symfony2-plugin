package fr.adrienbrault.idea.symfony2plugin.tests.translation;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.translation.PhpTranslationKeyInspection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see PhpTranslationKeyInspection
 */
public class PhpTranslationKeyInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("PhpTranslationInspection.php");
        myFixture.copyFileToProject("symfony.de.yml", "Resources/translations/symfony.de.yml");
        myFixture.copyFileToProject("messages.de.yml", "Resources/translations/messages.de.yml");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/translation/fixtures";
    }

    public void testThatPhpTransInspectionsAreProvided() {
        assertLocalInspectionContains("test.php", "<?php\n" +
            "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
            "$x->trans('fo<caret>obar', [], 'domain')",
            PhpTranslationKeyInspection.MESSAGE
        );

        assertLocalInspectionNotContains("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans('f$o<caret>obar', [], 'domain')",
            PhpTranslationKeyInspection.MESSAGE
        );

        assertLocalInspectionContains("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->transChoice('fo<caret>obar', 1, [], 'domain')",
            PhpTranslationKeyInspection.MESSAGE
        );

        assertLocalInspectionNotContains("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->transChoice('f$o<caret>obar', 1, [], 'domain')",
            PhpTranslationKeyInspection.MESSAGE
        );
    }

    public void testThatPhpTransInspectionsAreNotProvidedForKnownTranslations() {
        assertLocalInspectionContains("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans('symfon<caret>y.great')",
            PhpTranslationKeyInspection.MESSAGE
        );

        assertLocalInspectionNotContains("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans('symfon<caret>y.great', [], 'symfony')",
            PhpTranslationKeyInspection.MESSAGE
        );

        assertLocalInspectionNotContains("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->transChoice('symfon<caret>y.great', 1, [], 'symfony')",
            PhpTranslationKeyInspection.MESSAGE
        );

        assertLocalInspectionNotContains("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->transChoice('symfon<caret>y.great', 1, [], 'symfony', null)",
            PhpTranslationKeyInspection.MESSAGE
        );

        assertLocalInspectionNotContains("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans('symfony<caret>_message')",
            PhpTranslationKeyInspection.MESSAGE
        );

        assertLocalInspectionNotContains("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans('symfony<caret>_message', [])",
            PhpTranslationKeyInspection.MESSAGE
        );

        assertLocalInspectionNotContains("test.php", "<?php\n" +
                "$x = 'symfony';\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans('symfon<caret>y.great', [], $x)",
            PhpTranslationKeyInspection.MESSAGE
        );

        assertLocalInspectionNotContains("test.php", "<?php\n" +
                "$x = 'symfony';\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->transChoice('symfon<caret>y.great', 1, [], $x)",
            PhpTranslationKeyInspection.MESSAGE
        );
    }
}
