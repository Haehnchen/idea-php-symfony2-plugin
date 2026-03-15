package fr.adrienbrault.idea.symfony2plugin.tests.templating

import com.intellij.patterns.PlatformPatterns
import com.jetbrains.php.lang.PhpFileType
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.TemplateRenderBlockCompletionContributor
 */
class TemplateRenderBlockCompletionContributorTest : SymfonyLightCodeInsightFixtureTestCase() {

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(
            "ide-twig.json",
            "{\"namespaces\":[{\"namespace\":\"\",\"path\":\"templates\"}]}"
        )
        myFixture.addFileToProject(
            "src/AbstractController.php",
            """<?php
namespace Symfony\Bundle\FrameworkBundle\Controller;
abstract class AbstractController {
    public function renderBlock(string ${'$'}view, string ${'$'}block, array ${'$'}parameters = []): string {}
    public function renderBlockView(string ${'$'}view, string ${'$'}block, array ${'$'}parameters = []): string {}
}
"""
        )
        myFixture.addFileToProject(
            "templates/base.html.twig",
            "{% block content %}base{% endblock %}{% block sidebar %}side{% endblock %}"
        )
    }

    /**
     * $this->renderBlock('base.html.twig', '<caret>') should complete block names
     */
    fun testRenderBlockProvidesBlockNameCompletion() {
        assertCompletionContains(
            PhpFileType.INSTANCE,
            """<?php
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
class MyController extends AbstractController {
    public function foo() { ${'$'}this->renderBlock('base.html.twig', '<caret>'); }
}""",
            "content", "sidebar"
        )
    }

    /**
     * $this->renderBlockView('base.html.twig', '<caret>') should complete block names
     */
    fun testRenderBlockViewProvidesBlockNameCompletion() {
        assertCompletionContains(
            PhpFileType.INSTANCE,
            """<?php
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
class MyController extends AbstractController {
    public function foo() { ${'$'}this->renderBlockView('base.html.twig', '<caret>'); }
}""",
            "content", "sidebar"
        )
    }

    /**
     * First parameter position should not trigger block completion
     */
    fun testFirstParameterDoesNotProvideBlockCompletion() {
        assertCompletionNotContains(
            PhpFileType.INSTANCE,
            """<?php
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
class MyController extends AbstractController {
    public function foo() { ${'$'}this->renderBlock('<caret>', 'content'); }
}""",
            "content"
        )
    }

    /**
     * $this->renderBlock('base.html.twig', 'con<caret>tent') should navigate to block definition
     */
    fun testRenderBlockProvidesNavigationToBlockDefinition() {
        assertNavigationMatch(
            PhpFileType.INSTANCE,
            """<?php
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
class MyController extends AbstractController {
    public function foo() { ${'$'}this->renderBlock('base.html.twig', 'con<caret>tent'); }
}""",
            PlatformPatterns.psiElement()
        )
    }

    /**
     * Navigation from renderBlock should also work for blocks in parent templates
     */
    fun testRenderBlockProvidesNavigationToParentBlockDefinition() {
        myFixture.addFileToProject(
            "templates/child.html.twig",
            "{% extends 'base.html.twig' %}{% block extra %}child{% endblock %}"
        )

        assertNavigationMatch(
            PhpFileType.INSTANCE,
            """<?php
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
class MyController extends AbstractController {
    public function foo() { ${'$'}this->renderBlock('child.html.twig', 'con<caret>tent'); }
}""",
            PlatformPatterns.psiElement()
        )
    }
}
