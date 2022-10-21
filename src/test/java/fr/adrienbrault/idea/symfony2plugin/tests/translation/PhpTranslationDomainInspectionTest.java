package fr.adrienbrault.idea.symfony2plugin.tests.translation;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.translation.PhpTranslationDomainInspection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see PhpTranslationDomainInspection
 */
public class PhpTranslationDomainInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("PhpTranslationInspection.php");
        myFixture.copyFileToProject("symfony.de.yml", "Resources/translations/symfony.de.yml");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/translation/fixtures";
    }

    public void testThatPhpTranslationDomainInspectionsAreProvided() {
        assertLocalInspectionContains("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans('foobar', [], 'dom<caret>ain')",
            PhpTranslationDomainInspection.MESSAGE
        );

        assertLocalInspectionContains("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->transChoice('foobar', 1, [], 'do<caret>main')",
            PhpTranslationDomainInspection.MESSAGE
        );

        assertLocalInspectionNotContains("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans('foobar', [], 'sym<caret>fony')",
            PhpTranslationDomainInspection.MESSAGE
        );

        assertLocalInspectionNotContains("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->transChoice('foobar', 1, [], 'sym<caret>fony')",
            PhpTranslationDomainInspection.MESSAGE
        );

        assertLocalInspectionNotContains("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans('foo<caret>bar')",
            PhpTranslationDomainInspection.MESSAGE
        );

        assertLocalInspectionContains("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans('id', domain: 'dom<caret>ain')",
            PhpTranslationDomainInspection.MESSAGE
        );
    }

    public void testThatPhpTranslationDomainInspectionsForTranslatableMessageAreProvided() {
        assertLocalInspectionContains("test.php", "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage('foobar', [], 'do<caret>main');",
            PhpTranslationDomainInspection.MESSAGE
        );

        assertLocalInspectionContains("test.php", "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage('foobar', [], 'foo<caret>bar');",
            PhpTranslationDomainInspection.MESSAGE
        );

        assertLocalInspectionNotContains("test.php", "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage('foobar', [], 'sym<caret>fony');",
            PhpTranslationDomainInspection.MESSAGE
        );

        assertLocalInspectionNotContains("test.php", "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage(domain: 'sym<caret>fony');",
            PhpTranslationDomainInspection.MESSAGE
        );
    }
}
