package fr.adrienbrault.idea.symfony2plugin.tests.form.intention

import com.jetbrains.php.lang.PhpFileType
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.form.intention.FormStringToClassConstantIntention
 */
class FormStringToClassConstantIntentionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"))
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/form/intention/fixtures"
    }

    fun testIntentionIsAvailable() {
        assertIntentionIsAvailable(
            PhpFileType.INSTANCE,
            "<?php\n /** @var \$foo \\Symfony\\Component\\Form\\FormBuilderInterface */\n \$foo->add('', 'hid<caret>den')",
            "Symfony: use FormType class constant",
        )
    }
}
