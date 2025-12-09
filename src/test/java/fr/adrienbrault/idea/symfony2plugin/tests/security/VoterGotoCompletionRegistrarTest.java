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

    public void testTwigIsGrantedForUserCompletion() {
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ is_granted_for_user(user, '<caret>') }}",
            "YAML_ROLE_USER_FOOBAR"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{% if is_granted_for_user(another_user, '<caret>') %}",
            "YAML_ROLE_USER_FOOBAR"
        );
    }

    public void testTwigIsGrantedForUserNavigation() {
        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{{ is_granted_for_user(user, 'YAML_ROLE<caret>_USER_FOOBAR') }}",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% if is_granted_for_user(another_user, 'YAML_ROLE<caret>_USER_FOOBAR') %}",
            PlatformPatterns.psiElement()
        );
    }

    public void testTwigIsGrantedForUserAsArrayCompletion() {
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ is_granted_for_user(user, ['<caret>']) }}",
            "YAML_ROLE_USER_FOOBAR"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ is_granted_for_user(user, {'<caret>'}) }}",
            "YAML_ROLE_USER_FOOBAR"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ is_granted_for_user(user, ['foobar', '<caret>']) }}",
            "YAML_ROLE_USER_FOOBAR"
        );
    }

    public void testTwigIsGrantedForUserAsArrayNavigation() {
        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{{ is_granted_for_user(user, ['YAML_ROLE<caret>_USER_FOOBAR']) }}",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{{ is_granted_for_user(user, {'YAML_ROLE<caret>_USER_FOOBAR'}) }}",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{{ is_granted_for_user(user, ['foobar', 'YAML_ROLE<caret>_USER_FOOBAR']) }}",
            PlatformPatterns.psiElement()
        );
    }

    public void testPhpIsGrantedForUserCompletion() {
        assertCompletionContains(PhpFileType.INSTANCE,"<?php\n" +
                "/** @var $x \\Symfony\\Component\\Security\\Core\\Authorization\\UserAuthorizationCheckerInterface */\n" +
                "/** @var $user \\Symfony\\Component\\Security\\Core\\User\\UserInterface */\n" +
                "$x->isGrantedForUser($user, '<caret>');",
            "YAML_ROLE_USER_FOOBAR"
        );

        assertCompletionContains(PhpFileType.INSTANCE,"<?php\n" +
                "/** @var $x \\Symfony\\Component\\Security\\Core\\Authorization\\UserAuthorizationCheckerInterface */\n" +
                "/** @var $user \\Symfony\\Component\\Security\\Core\\User\\UserInterface */\n" +
                "$x->isGrantedForUser($user, [null, '<caret>']);",
            "YAML_ROLE_USER_FOOBAR"
        );
    }

    public void testPhpIsGrantedForUserNavigation() {
        assertNavigationMatch(PhpFileType.INSTANCE,"<?php\n" +
                "/** @var $x \\Symfony\\Component\\Security\\Core\\Authorization\\UserAuthorizationCheckerInterface */\n" +
                "/** @var $user \\Symfony\\Component\\Security\\Core\\User\\UserInterface */\n" +
                "$x->isGrantedForUser($user, 'YAML_ROLE_USE<caret>R_FOOBAR');",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch(PhpFileType.INSTANCE,"<?php\n" +
                "/** @var $x \\Symfony\\Component\\Security\\Core\\Authorization\\UserAuthorizationCheckerInterface */\n" +
                "/** @var $user \\Symfony\\Component\\Security\\Core\\User\\UserInterface */\n" +
                "$x->isGrantedForUser($user, [null, 'YAML_ROLE_USE<caret>R_FOOBAR']);",
            PlatformPatterns.psiElement()
        );
    }

    public void testTwigAccessDecisionCompletion() {
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{% set voter_decision = access_decision('<caret>', post) %}",
            "YAML_ROLE_USER_FOOBAR"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ access_decision('<caret>') }}",
            "YAML_ROLE_USER_FOOBAR"
        );
    }

    public void testTwigAccessDecisionNavigation() {
        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% set voter_decision = access_decision('YAML_ROLE<caret>_USER_FOOBAR', post) %}",
            PlatformPatterns.psiElement()
        );
    }

    public void testTwigAccessDecisionAsArrayCompletion() {
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ access_decision(['<caret>']) }}",
            "YAML_ROLE_USER_FOOBAR"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ access_decision({'<caret>'}) }}",
            "YAML_ROLE_USER_FOOBAR"
        );
    }

    public void testTwigAccessDecisionAsArrayNavigation() {
        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{{ access_decision(['YAML_ROLE<caret>_USER_FOOBAR']) }}",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{{ access_decision({'YAML_ROLE<caret>_USER_FOOBAR'}) }}",
            PlatformPatterns.psiElement()
        );
    }

    public void testTwigAccessDecisionForUserCompletion() {
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{% set voter_decision = access_decision_for_user(user, '<caret>', post) %}",
            "YAML_ROLE_USER_FOOBAR"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ access_decision_for_user(another_user, '<caret>') }}",
            "YAML_ROLE_USER_FOOBAR"
        );
    }

    public void testTwigAccessDecisionForUserNavigation() {
        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{% set voter_decision = access_decision_for_user(user, 'YAML_ROLE<caret>_USER_FOOBAR', post) %}",
            PlatformPatterns.psiElement()
        );
    }

    public void testTwigAccessDecisionForUserAsArrayCompletion() {
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ access_decision_for_user(user, ['<caret>']) }}",
            "YAML_ROLE_USER_FOOBAR"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{{ access_decision_for_user(user, {'<caret>'}) }}",
            "YAML_ROLE_USER_FOOBAR"
        );
    }

    public void testTwigAccessDecisionForUserAsArrayNavigation() {
        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{{ access_decision_for_user(user, ['YAML_ROLE<caret>_USER_FOOBAR']) }}",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{{ access_decision_for_user(user, {'YAML_ROLE<caret>_USER_FOOBAR'}) }}",
            PlatformPatterns.psiElement()
        );
    }
}
