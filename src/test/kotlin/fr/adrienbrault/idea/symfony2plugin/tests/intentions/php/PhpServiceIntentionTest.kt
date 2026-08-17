package fr.adrienbrault.idea.symfony2plugin.tests.intentions.php

import com.jetbrains.php.lang.PhpFileType
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.intentions.php.PhpServiceIntention
 */
class PhpServiceIntentionTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testIntentionIsAvailableInsideClassMethod() {
        assertIntentionIsAvailable(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Service;\n" +
                "class Foobar\n" +
                "{\n" +
                "    public function create(): void\n" +
                "    {\n" +
                "        <caret>\n" +
                "    }\n" +
                "}\n",
            "Generate Symfony service"
        )
    }

    fun testIntentionIsNotAvailableOutsideMethod() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace App\\Service;\n" +
                "class <caret>Foobar\n" +
                "{\n" +
                "}\n"
        )

        assertTrue(myFixture.filterAvailableIntentions("Generate Symfony service").isEmpty())
    }
}
