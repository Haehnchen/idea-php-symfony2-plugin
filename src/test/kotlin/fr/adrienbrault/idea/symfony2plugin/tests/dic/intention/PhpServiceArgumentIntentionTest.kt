package fr.adrienbrault.idea.symfony2plugin.tests.dic.intention

import com.jetbrains.php.lang.PhpFileType
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.intention.PhpServiceArgumentIntention
 */
class PhpServiceArgumentIntentionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("services.yml")
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/intention/fixtures"
    }

    fun testIntentionIsAvailable() {
        assertIntentionIsAvailable(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "" +
                "namespace Foo;\n" +
                "" +
                "class Foobar\n" +
                "{\n" +
                "<caret>" +
                "}\n",
            "Symfony: Update service arguments",
        )
    }
}
