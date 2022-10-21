package fr.adrienbrault.idea.symfony2plugin.tests.translation;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.translation.TranslationReferenceContributor
 */
public class TranslationReferenceContributorTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("PhpTranslationInspection.php");
        myFixture.copyFileToProject("symfony.de.yml", "Resources/translations/symfony.de.yml");
        myFixture.copyFileToProject("messages.de.yml", "Resources/translations/messages.de.yml");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/translation/fixtures";
    }

    public void testThatPhpTransDomainProvidesNavigation() {
        assertReferenceMatchOnParent(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans('foobar', [], 'sym<caret>fony')",
            PlatformPatterns.psiFile()
        );

        assertReferenceMatchOnParent(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans('foobar', domain: 'sym<caret>fony')",
            PlatformPatterns.psiFile()
        );
    }

    public void testThatPhpTranslatableMessageDomainProvidesNavigation() {
        assertReferenceMatchOnParent(PhpFileType.INSTANCE, "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage('foobar', [], 'sym<caret>fony');",
            PlatformPatterns.psiFile()
        );

        assertReferenceMatchOnParent(PhpFileType.INSTANCE, "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage(domain: 'sym<caret>fony');",
            PlatformPatterns.psiFile()
        );

        assertReferenceMatchOnParent(PhpFileType.INSTANCE, "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage(id: 'foobar', domain: 'symf<caret>ony');",
            PlatformPatterns.psiFile()
        );
    }

    public void testThatPhpTransKeyProvidesNavigation() {
        assertReferenceMatchOnParent("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans('symfony.gr<caret>eat', [], 'symfony')",
            PlatformPatterns.psiElement()
        );

        assertReferenceMatchOnParent("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans('symfony<caret>_message')",
            PlatformPatterns.psiElement()
        );

        assertReferenceMatchOnParent("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans('symfony.gr<caret>eat', domain: 'symfony')",
            PlatformPatterns.psiElement()
        );

        assertReferenceMatchOnParent("test.php", "<?php\n" +
                "/** @var $x Symfony\\Component\\Translation\\TranslatorInterface */" +
                "$x->trans(id: 'symfony.gr<caret>eat', domain: 'symfony')",
            PlatformPatterns.psiElement()
        );
    }

    public void testThatPhpTranslatableMessageKeyProvidesNavigation() {
        assertReferenceMatchOnParent("test.php", "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage('symfony.gr<caret>eat', [], 'symfony');",
            PlatformPatterns.psiElement()
        );

        assertReferenceMatchOnParent("test.php", "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage('symfony.gr<caret>eat', domain: 'symfony');",
            PlatformPatterns.psiElement()
        );

        assertReferenceMatchOnParent("test.php", "<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage('symfony<caret>_message');",
            PlatformPatterns.psiElement()
        );
    }
}
