package fr.adrienbrault.idea.symfony2plugin.tests.translation;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationKeyTargetFakePsiNavigationItem;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.translation.TranslationPlaceholderGotoCompletionRegistrar
 */
public class TranslationPlaceholderGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {
    private static final String HTML_TRANSLATOR = "<?php /** @var \\Symfony\\Component\\Translation\\HtmlTranslator $translator */ ";

    public void setUp() throws Exception {
        super.setUp();
        myFixture.addFileToProject("translations/messages.en.yaml", "default_prompt: 'Hello %name% <link>sign in</link>'");
        myFixture.addFileToProject("translations/auth+intl-icu.en.yaml", "auth_prompt: 'Hello {name}! <signin_link>Sign in</signin_link>'");
        myFixture.copyFileToProject("HtmlTranslator.php");
        myFixture.copyFileToProject("messages.de.yml", "Resources/translations/messages.de.yml");
        myFixture.copyFileToProject("symfony.de.yml", "Resources/translations/symfony.de.yml");
        myFixture.copyFileToProject("TranslationPlaceholderGotoCompletionRegistrar.php");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/translation/fixtures";
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

    public void testCompletionForPhpTranslatableMessagePlaceholder() {
        assertCompletionContains(
            PhpFileType.INSTANCE,"<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage('symfony.great', ['<caret>'], 'symfony');",
            "%foobar%"
        );

        assertCompletionContains(
            PhpFileType.INSTANCE,"<?php\n" +
            "new \\Symfony\\Component\\Translation\\TranslatableMessage('symfony.great', ['<caret>', null], 'symfony');",
            "%foobar%"
        );

        assertCompletionContains(
            PhpFileType.INSTANCE,"<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage('symfony_message', ['<caret>']);",
            "%foobar%"
        );

        assertCompletionContains(
            PhpFileType.INSTANCE,"<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage('symfony_message', ['<caret>' => 'a']);",
            "%foobar%"
        );
    }

    public void testNavigationForPhpTranslatableMessagePlaceholder() {
        assertNavigationMatch(
            PhpFileType.INSTANCE,"<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage('symfony.great', ['%fo<caret>obar%'], 'symfony');",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch(
            PhpFileType.INSTANCE,"<?php\n" +
                "new \\Symfony\\Component\\Translation\\TranslatableMessage('symfony.great', ['%fo<caret>obar%', null], 'symfony');",
            PlatformPatterns.psiElement()
        );
    }

    public void testNavigationForPhpTranslatableMessageTFunctionPlaceholder() {
        assertNavigationMatch(
            PhpFileType.INSTANCE,"<?php\n" +
                "use function Symfony\\Component\\Translation\\t;\n" +
                "t('symfony.great', ['%fo<caret>obar%'], 'symfony');",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch(
            PhpFileType.INSTANCE,"<?php\n" +
                "use function Symfony\\Component\\Translation\\t;\n" +
                "t('symfony.great', ['%fo<caret>obar%', null], 'symfony');",
            PlatformPatterns.psiElement()
        );
    }

    public void testTwigTransHtmlPlaceholderCompletion() {
        assertCompletionContains(TwigFileType.INSTANCE, "{{ 'auth_prompt'|trans_html({}, {'<caret>': user.name}, 'auth') }}", "name");
        assertCompletionContains(TwigFileType.INSTANCE, "{{ 'auth_prompt'|trans_html(domain: 'auth', arguments: {'<caret>': user.name}, tags: {}) }}", "name");
        assertCompletionContains(TwigFileType.INSTANCE, "{{ 'auth_prompt'|trans_html(domain: 'auth', arguments: {<caret>}, tags: {}) }}", "name");
        assertCompletionContains(TwigFileType.INSTANCE, "{{ 'default_prompt'|trans_html({}, {'<caret>': user.name}) }}", "%name%");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ 'auth_prompt'|trans_html({'<caret>': {}}, {}, 'auth') }}", "name");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ 'auth_prompt'|trans_html({}, {'name': '<caret>'}, 'auth') }}", "name");
    }

    public void testTwigTransHtmlPlaceholderNavigation() {
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ 'auth_prompt'|trans_html(arguments: {'na<caret>me': user.name}, tags: {}, domain: 'auth') }}", PlatformPatterns.psiElement(TranslationKeyTargetFakePsiNavigationItem.class));
    }

    public void testTwigTransHtmlHtmlTagCompletionAndNavigation() {
        assertCompletionContains(TwigFileType.INSTANCE, "{{ 'auth_prompt'|trans_html({'<caret>': {}}, {}, 'auth') }}", "signin_link");
        assertCompletionContains(TwigFileType.INSTANCE, "{{ 'auth_prompt'|trans_html(domain: 'auth', tags: {<caret>}) }}", "signin_link");
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ 'auth_prompt'|trans_html(domain: 'auth', tags: {'signin_<caret>link': {tag: 'a'}}) }}", PlatformPatterns.psiElement(TranslationKeyTargetFakePsiNavigationItem.class));
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ 'auth_prompt'|trans_html(domain: 'auth', tags: {signin_link: {'<caret>': 'a'}}) }}", "signin_link");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ 'auth_prompt'|trans_html(domain: 'auth', arguments: {'<caret>': 'Daniel'}, tags: {}) }}", "signin_link");
    }

    public void testPhpHtmlTranslatorHtmlTagCompletionAndNavigation() {
        assertCompletionContains(PhpFileType.INSTANCE, HTML_TRANSLATOR + "$translator->trans('auth_prompt', ['<caret>' => []], [], 'auth');", "signin_link");
        assertCompletionContains(PhpFileType.INSTANCE, HTML_TRANSLATOR + "$translator->trans(domain: 'auth', tags: ['<caret>' => []], id: 'auth_prompt');", "signin_link");
        assertNavigationMatch(PhpFileType.INSTANCE, HTML_TRANSLATOR + "$translator->trans('auth_prompt', ['signin_<caret>link' => ['tag' => 'a']], [], 'auth');", PlatformPatterns.psiElement(TranslationKeyTargetFakePsiNavigationItem.class));
        assertCompletionNotContains(PhpFileType.INSTANCE, HTML_TRANSLATOR + "$translator->trans('auth_prompt', ['signin_link' => ['<caret>' => 'a']], [], 'auth');", "signin_link");
    }

    public void testHtmlTagCompletionFromXliff() {
        myFixture.addFileToProject("translations/emails.en.xlf", """
            <?xml version="1.0"?>
            <xliff version="1.2"><file source-language="en" datatype="plaintext" original="file.ext"><body>
            <trans-unit id="welcome"><source>welcome</source><target><![CDATA[<login-link>Welcome</login-link><line_break/><span class="bad">Ignored</span>]]></target></trans-unit>
            </body></file></xliff>
            """);
        assertCompletionContains(TwigFileType.INSTANCE, "{{ 'welcome'|trans_html({'<caret>': {}}, {}, 'emails') }}", "login-link", "line_break");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ 'welcome'|trans_html({'<caret>': {}}, {}, 'emails') }}", "span");
    }

    public void testPhpHtmlTranslatorPlaceholderCompletion() {
        assertCompletionContains(PhpFileType.INSTANCE, HTML_TRANSLATOR + "$translator->trans('auth_prompt', [], ['<caret>' => 'Daniel'], 'auth');", "name");
        assertCompletionContains(PhpFileType.INSTANCE, HTML_TRANSLATOR + "$translator->trans(domain: 'auth', parameters: ['<caret>' => 'Daniel'], id: 'auth_prompt', tags: []);", "name");
        assertCompletionNotContains(PhpFileType.INSTANCE, HTML_TRANSLATOR + "$translator->trans('auth_prompt', ['<caret>' => []], [], 'auth');", "name");
        assertCompletionNotContains(PhpFileType.INSTANCE, HTML_TRANSLATOR + "$translator->trans('auth_prompt', [], ['name' => '<caret>'], 'auth');", "name");
    }

    public void testPhpHtmlTranslatorPlaceholderNavigation() {
        assertNavigationMatch(PhpFileType.INSTANCE, HTML_TRANSLATOR + "$translator->trans('auth_prompt', [], ['na<caret>me' => 'Daniel'], 'auth');", PlatformPatterns.psiElement(TranslationKeyTargetFakePsiNavigationItem.class));
    }
}
