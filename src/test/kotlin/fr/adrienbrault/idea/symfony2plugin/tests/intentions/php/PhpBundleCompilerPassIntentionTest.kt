package fr.adrienbrault.idea.symfony2plugin.tests.intentions.php

import com.jetbrains.php.lang.PhpFileType
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.intentions.php.PhpBundleCompilerPassIntention
 */
class PhpBundleCompilerPassIntentionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"))
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/intentions/php/fixtures"
    }

    fun testIntentionIsAvailableForBundleClass() {
        assertIntentionIsAvailable(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App;\n" +
                "use Symfony\\Component\\HttpKernel\\Bundle\\BundleInterface;\n" +
                "class <caret>AppBundle implements BundleInterface\n" +
                "{\n" +
                "}\n",
            "Symfony: Create CompilerPass"
        )
    }

    fun testIntentionIsNotAvailableForNonBundleClass() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App;\n" +
                "class <caret>AppService\n" +
                "{\n" +
                "}\n"
        )

        assertTrue(myFixture.filterAvailableIntentions("Symfony: Create CompilerPass").isEmpty())
    }
}
