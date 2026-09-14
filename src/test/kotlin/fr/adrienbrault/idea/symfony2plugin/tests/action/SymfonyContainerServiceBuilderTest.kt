package fr.adrienbrault.idea.symfony2plugin.tests.action

import fr.adrienbrault.idea.symfony2plugin.action.SymfonyContainerServiceBuilder
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class SymfonyContainerServiceBuilderTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testActionAvailableForFileScope() {
        myFixture.configureByText(
            "test.php",
            "<?php\n" +
                "class Foobar {}"
        )

        assertTrue(myFixture.testAction(SymfonyContainerServiceBuilder()).isEnabledAndVisible)
    }

    fun testActionAvailableForXmlFile() {
        myFixture.configureByText("test.xml", "")
        assertTrue(myFixture.testAction(SymfonyContainerServiceBuilder()).isEnabledAndVisible)
    }

    fun testActionAvailableForYmlFile() {
        myFixture.configureByText("test.yml", "")
        assertTrue(myFixture.testAction(SymfonyContainerServiceBuilder()).isEnabledAndVisible)
    }
}
