package fr.adrienbrault.idea.symfony2plugin.tests.security;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.security.VoterGotoCompletionRegistrar
 */
public class VoterGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("security.yml");
        myFixture.copyFileToProject("classes.php");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/security/fixtures";
    }

    public void testTwigIsGrantedCompletion() {
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ is_granted('<caret>') }}",
            "YAML_ROLE_USER_FOOBAR"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{% if is_granted('<caret>') %}",
            "YAML_ROLE_USER_FOOBAR"
        );
    }

    public void testTwigIsGrantedNavigation() {
        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{{ is_granted('YAML_ROLE<caret>_USER_FOOBAR') }}",
            PlatformPatterns.psiElement()
        );
    }

    public void testTwigIsGrantedAsArrayCompletion() {
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ is_granted(['<caret>']) }}",
            "YAML_ROLE_USER_FOOBAR"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ is_granted({'<caret>'}) }}",
            "YAML_ROLE_USER_FOOBAR"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ is_granted(['foobar', '<caret>']) }}",
            "YAML_ROLE_USER_FOOBAR"
        );
    }

    public void testTwigIsGrantedAsArrayNavigation() {
        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{{ is_granted(['YAML_ROLE<caret>_USER_FOOBAR']) }}",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{{ is_granted({'YAML_ROLE<caret>_USER_FOOBAR'}) }}",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{{ is_granted(['foobar', 'YAML_ROLE<caret>_USER_FOOBAR']) }}",
            PlatformPatterns.psiElement()
        );
    }

    public void testPhpIsGrantedCompletion() {
        assertCompletionContains(PhpFileType.INSTANCE,"<?php\n" +
                "/** @var $x \\Symfony\\Component\\Security\\Core\\Authorization\\AuthorizationCheckerInterface */\n" +
                "$x->isGranted('<caret>');",
            "YAML_ROLE_USER_FOOBAR"
        );

        assertCompletionContains(PhpFileType.INSTANCE,"<?php\n" +
                "/** @var $x \\Symfony\\Component\\Security\\Core\\Authorization\\AuthorizationCheckerInterface */\n" +
                "$x->isGranted([null, '<caret>']);",
            "YAML_ROLE_USER_FOOBAR"
        );
    }

    public void testPhpIsGrantedNavigation() {
        assertNavigationMatch(PhpFileType.INSTANCE,"<?php\n" +
                "/** @var $x \\Symfony\\Component\\Security\\Core\\Authorization\\AuthorizationCheckerInterface */\n" +
                "$x->isGranted('YAML_ROLE_USE<caret>R_FOOBAR');",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch(PhpFileType.INSTANCE,"<?php\n" +
                "/** @var $x \\Symfony\\Component\\Security\\Core\\Authorization\\AuthorizationCheckerInterface */\n" +
                "$x->isGranted([null, 'YAML_ROLE_USE<caret>R_FOOBAR']);",
            PlatformPatterns.psiElement()
        );
    }
}
