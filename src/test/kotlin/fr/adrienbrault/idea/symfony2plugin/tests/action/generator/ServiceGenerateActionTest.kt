package fr.adrienbrault.idea.symfony2plugin.tests.action.generator

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.testFramework.TestActionEvent
import com.jetbrains.php.lang.PhpFileType
import fr.adrienbrault.idea.symfony2plugin.action.generator.ServiceGenerateAction
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see ServiceGenerateAction
 */
class ServiceGenerateActionTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testActionAvailableInsidePhpClass() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\nclass Foo { private \$fo<caret>o; }"
        )

        val action = ServiceGenerateAction()
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
