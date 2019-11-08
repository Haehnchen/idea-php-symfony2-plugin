package fr.adrienbrault.idea.symfonyplugin.tests.translation;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfonyplugin.translation.TranslationPlaceholderGotoCompletionRegistrar
 */
public class TranslationPlaceholderGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("symfony.de.yml", "Resources/translations/symfony.de.yml");
        myFixture.copyFileToProject("TranslationPlaceholderGotoCompletionRegistrar.php");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/translation/fixtures";
    }

    public void testCompletionForTwigTransPlaceholder() {
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ 'symfony.great'|trans({'fo<caret>f'}, 'symfony')) }}",
            "%foobar%"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ 'symfony.great'|trans({'foobar': 'foobar', 'fo<caret>f'}, 'symfony')) }}",
            "%foobar%"
        );
    }

    public void testNavigationForTwigTransPlaceholder() {
        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{{ 'symfony.great'|trans({'foobar': 'foobar', '%foo<caret>bar%'}, 'symfony')) }}",
            PlatformPatterns.psiElement()
        );
    }

    public void testCompletionForTwigTransChoicePlaceholder() {
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ 'symfony.great'|transchoice(12, {'fo<caret>f'}, 'symfony')) }}",
            "%foobar%"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ 'symfony.great'|transchoice(12, {'foobar': 'foobar', 'fo<caret>f'}, 'symfony')) }}",
            "%foobar%"
        );
    }

    public void testNavigationForTwigTransChoicePlaceholder() {
        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{{ 'symfony.great'|transchoice(12, {'foobar': 'foobar', '%foo<caret>bar%'}, 'symfony')) }}",
            PlatformPatterns.psiElement()
        );
    }

    public void testCompletionForPhpTransPlaceholder() {
        assertCompletionContains(
            PhpFileType.INSTANCE,"<?php\n" +
                "/** @var $x \\Symfony\\Component\\Translation\\TranslatorInterface */\n" +
                "$x->trans('symfony.great', ['<caret>'], 'symfony')",
            "%foobar%"
        );

        assertCompletionContains(
            PhpFileType.INSTANCE,"<?php\n" +
                "/** @var $x \\Symfony\\Component\\Translation\\TranslatorInterface */\n" +
                "$x->trans('symfony.great', ['<caret>', null], 'symfony')",
            "%foobar%"
        );
    }

    public void testNavigationForPhpTransPlaceholder() {
        assertNavigationMatch(
            PhpFileType.INSTANCE,"<?php\n" +
                "/** @var $x \\Symfony\\Component\\Translation\\TranslatorInterface */\n" +
                "$x->trans('symfony.great', ['%fo<caret>obar%'], 'symfony')",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch(
            PhpFileType.INSTANCE,"<?php\n" +
                "/** @var $x \\Symfony\\Component\\Translation\\TranslatorInterface */\n" +
                "$x->trans('symfony.great', ['%fo<caret>obar%', null], 'symfony')",
            PlatformPatterns.psiElement()
        );
    }

    public void testCompletionForPhpTransChoicePlaceholder() {
        assertCompletionContains(
            PhpFileType.INSTANCE,"<?php\n" +
                "/** @var $x \\Symfony\\Component\\Translation\\TranslatorInterface */\n" +
                "$x->transChoice('symfony.great', 2, ['<caret>'], 'symfony')",
            "%foobar%"
        );

        assertCompletionContains(
            PhpFileType.INSTANCE,"<?php\n" +
                "/** @var $x \\Symfony\\Component\\Translation\\TranslatorInterface */\n" +
                "$x->transChoice('symfony.great', 2, ['<caret>', null], 'symfony')",
            "%foobar%"
        );
    }

    public void testNavigationForPhpTransChoicePlaceholder() {
        assertNavigationMatch(
            PhpFileType.INSTANCE,"<?php\n" +
                "/** @var $x \\Symfony\\Component\\Translation\\TranslatorInterface */\n" +
                "$x->transChoice('symfony.great', 2, ['%fo<caret>obar%'], 'symfony')",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch(
            PhpFileType.INSTANCE,"<?php\n" +
                "/** @var $x \\Symfony\\Component\\Translation\\TranslatorInterface */\n" +
                "$x->transChoice('symfony.great', 2, ['%fo<caret>obar%', null], 'symfony')",
            PlatformPatterns.psiElement()
        );
    }
}
