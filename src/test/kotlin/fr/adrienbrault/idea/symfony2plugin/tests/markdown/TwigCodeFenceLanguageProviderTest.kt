package fr.adrienbrault.idea.symfony2plugin.tests.markdown

import com.intellij.lang.LanguageUtil
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.patterns.PlatformPatterns
import com.jetbrains.twig.TwigHighlighter
import com.jetbrains.twig.TwigLanguage
import fr.adrienbrault.idea.symfony2plugin.markdown.MarkdownTwigLanguage
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

class TwigCodeFenceLanguageProviderTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(
            "config/routes.xml",
            """
            <routes xmlns="http://symfony.com/schema/routing">
                <route id="FOOBAR" path="/foobar"/>
            </routes>
            """.trimIndent(),
        )
    }

    fun testTwigFenceLanguageIsOfferedInMarkdownCompletion() {
        myFixture.configureByText("README.md", "```tw<caret>\n\n```")
        myFixture.completeBasic()

        assertContainsElements(myFixture.lookupElementStrings.orEmpty(), "twig")
    }

    fun testTwigIsInjectedIntoMarkdownCodeFence() {
        myFixture.configureByText(
            "README.md",
            "```twig\n{% block tit<caret>le %}Home{% endblock %}\n```",
        )

        val injectedFile = myFixture.file
        val injectedElement = injectedFile.findElementAt(myFixture.caretOffset)

        assertSame(MarkdownTwigLanguage, injectedFile.language)
        assertNotNull(injectedFile.context)
        assertNotNull(injectedElement)
        assertTrue(injectedElement!!.language.isKindOf(TwigLanguage.INSTANCE))
    }

    fun testTwigCompletionWorksInsideMarkdownCodeFence() {
        myFixture.configureByText("README.md", "```twig\n{% bl<caret> %}\n```")
        myFixture.completeBasic()

        assertContainsElements(myFixture.lookupElementStrings.orEmpty(), "block")
    }

    fun testSymfonyRouteCompletionWorksInsideMarkdownCodeFence() {
        myFixture.configureByText("README.md", "```twig\n{{ path('FO<caret>') }}\n```")
        myFixture.completeBasic()

        assertContainsElements(myFixture.lookupElementStrings.orEmpty(), "FOOBAR")
    }

    fun testSymfonyRouteNavigationWorksInsideMarkdownCodeFence() {
        myFixture.configureByText("README.md", "```twig\n{{ path('FOO<caret>BAR') }}\n```")

        assertNavigationMatch(PlatformPatterns.psiElement())
    }

    fun testMarkdownTwigDialectIsInjectable() {
        assertTrue(LanguageUtil.isInjectableLanguage(MarkdownTwigLanguage))
        assertTrue(MarkdownTwigLanguage.isKindOf(TwigLanguage.INSTANCE))
    }

    fun testMarkdownPreviewUsesTwigSyntaxHighlighter() {
        val highlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(MarkdownTwigLanguage, project, null)

        assertInstanceOf(highlighter, TwigHighlighter.TwigFileHighlighter::class.java)
    }
}
