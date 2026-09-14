package fr.adrienbrault.idea.symfony2plugin.tests.action

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.testFramework.TestActionEvent
import fr.adrienbrault.idea.symfony2plugin.action.TwigExtractLanguageAction
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigExtractLanguageAction
 */
class TwigExtractLanguageActionTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testActionAvailableForTwigText() {
        myFixture.configureByText("foo.html.twig", "Hello <caret>world")

        val action = TwigExtractLanguageAction()
        val event = TestActionEvent.createTestEvent(
            action,
            SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, project)
                .add(CommonDataKeys.EDITOR, myFixture.editor)
                .add(CommonDataKeys.PSI_FILE, myFixture.file)
                .build()
        )
        action.update(event)

        assertTrue(event.presentation.isEnabledAndVisible)
    }
}
