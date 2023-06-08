package fr.adrienbrault.idea.symfony2plugin.tests.translation;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.translation.TranslationNavigationCompletionContributor
 */
public class TranslationNavigationCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("PhpTranslationInspection.php");
        myFixture.copyFileToProject("symfony.de.yml", "Resources/translations/symfony.de.yml");
        myFixture.copyFileToProject("messages.de.yml", "Resources/translations/messages.de.yml");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/translation/fixtures";
    }

    public void testThatPhpTransDomainProvidesCompletion() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans('foobar', [], '<caret>');",
            "symfony"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans('foobar', domain: '<caret>');",
            "symfony"
        );
    }

    public void testThatPhpTransDomainProvidesNavigation() {
        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans('foobar', [], 'sym<caret>fony');",
            PlatformPatterns.psiFile()
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans('foobar', domain: 'sym<caret>fony');",
            PlatformPatterns.psiFile()
        );
    }

    public void testThatPhpTranslatableMessageDomainProvidesCompletion() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage('foobar', [], '<caret>');",
            "symfony"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage(domain: '<caret>');",
            "symfony"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage(id: 'foobar', domain: '<caret>');",
            "symfony"
        );
    }

    public void testThatPhpTranslatableMessageDomainProvidesNavigation() {
        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage('foobar', [], 'sym<caret>fony');",
            PlatformPatterns.psiFile()
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage(domain: 'sym<caret>fony');",
            PlatformPatterns.psiFile()
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage(id: 'foobar', domain: 'symf<caret>ony');",
            PlatformPatterns.psiFile()
        );
    }

    public void testThatPhpTransKeyProvidesCompletion() {
        assertCompletionContains("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans('<caret>', [], 'symfony')",
            "symfony.great"
        );

        assertCompletionContains("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans('<caret>')",
            "symfony_message"
        );

        assertCompletionContains("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans('<caret>', domain: 'symfony')",
            "symfony.great"
        );

        assertCompletionContains("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans(id: '<caret>', domain: 'symfony')",
            "symfony.great"
        );
    }

    public void testThatPhpTransKeyProvidesNavigation() {
        assertNavigationMatch("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans('symfony.gr<caret>eat', [], 'symfony')",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans('symfony<caret>_message')",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans('symfony.gr<caret>eat', domain: 'symfony')",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans(id: 'symfony.gr<caret>eat', domain: 'symfony')",
            PlatformPatterns.psiElement()
        );
    }

    public void testThatPhpTranslatableMessageKeyProvidesCompletion() {
        assertCompletionContains("test.php", "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage('<caret>', [], 'symfony');",
            "symfony.great"
        );

        assertCompletionContains("test.php", "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage('<caret>', domain: 'symfony');",
            "symfony.great"
        );

        assertCompletionContains("test.php", "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage('<caret>');",
            "symfony_message"
        );
    }

    public void testThatPhpTranslatableMessageKeyProvidesNavigation() {
        assertNavigationMatch("test.php", "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage('symfony.gr<caret>eat', [], 'symfony');",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch("test.php", "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage('symfony.gr<caret>eat', domain: 'symfony');",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch("test.php", "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage('symfony<caret>_message');",
            PlatformPatterns.psiElement()
        );
    }
}
