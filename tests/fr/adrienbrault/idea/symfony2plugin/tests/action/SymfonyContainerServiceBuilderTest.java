package fr.adrienbrault.idea.symfony2plugin.tests.action;

import fr.adrienbrault.idea.symfony2plugin.action.SymfonyContainerServiceBuilder;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyContainerServiceBuilderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testActionAvailableForFileScope() {
        myFixture.configureByText("test.php", "<?php\n" +
            "class Foobar {}"
        );

        assertTrue(myFixture.testAction(new SymfonyContainerServiceBuilder()).isEnabledAndVisible());
    }
}
