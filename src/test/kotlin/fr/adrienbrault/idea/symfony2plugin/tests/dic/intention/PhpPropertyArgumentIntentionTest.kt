package fr.adrienbrault.idea.symfony2plugin.tests.dic.intention

import com.jetbrains.php.lang.PhpFileType
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @see fr.adrienbrault.idea.symfony2plugin.dic.intention.PhpPropertyArgumentIntention
 */
class PhpPropertyArgumentIntentionTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testIntentionIsAvailableForMissingPropertyOnService() {
        myFixture.addFileToProject(
            "services.yml",
            "services:\n  App\\Service\\Foobar: ~",
        )

        assertIntentionIsAvailable(
            PhpFileType.INSTANCE,
            "<?php namespace App\\Service; class Foobar {" +
                " public function run() { \$this->log<caret>ger->info('message'); }" +
                "}",
            "Symfony: Add Property Service",
        )
    }
}
