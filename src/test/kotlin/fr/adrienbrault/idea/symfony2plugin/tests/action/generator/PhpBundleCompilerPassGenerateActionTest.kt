package fr.adrienbrault.idea.symfony2plugin.tests.action.generator

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.testFramework.TestActionEvent
import com.jetbrains.php.lang.PhpFileType
import fr.adrienbrault.idea.symfony2plugin.action.generator.PhpBundleCompilerPassGenerateAction
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see PhpBundleCompilerPassGenerateAction
 */
class PhpBundleCompilerPassGenerateActionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(
            "BundleInterface.php",
            "<?php namespace Symfony\\Component\\HttpKernel\\Bundle; interface BundleInterface {}"
        )
    }

    fun testActionAvailableInsideBundleClass() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php class FooBundle implements \\Symfony\\Component\\HttpKernel\\Bundle\\BundleInterface { <caret> }"
        )

        val action = PhpBundleCompilerPassGenerateAction()
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
