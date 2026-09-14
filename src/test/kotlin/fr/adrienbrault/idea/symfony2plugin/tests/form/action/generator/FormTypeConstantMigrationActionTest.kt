package fr.adrienbrault.idea.symfony2plugin.tests.form.action.generator

import com.jetbrains.php.lang.PhpFileType
import fr.adrienbrault.idea.symfony2plugin.form.action.generator.FormTypeConstantMigrationAction
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see FormTypeConstantMigrationAction
 */
class FormTypeConstantMigrationActionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"))
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/form/action/generator/fixtures"
    }

    fun testActionAvailableForFileScope() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "" +
                "<?php\n" +
                "class Foo implements \\Symfony\\Component\\Form\\FormTypeInterface\n" +
                "{" +
                "  \n private \$foo = 'te<caret>st';\n" +
                "} "
        )

        assertTrue(myFixture.testAction(FormTypeConstantMigrationAction()).isEnabledAndVisible)
    }
}
