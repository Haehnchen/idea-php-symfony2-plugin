package fr.adrienbrault.idea.symfonyplugin.tests.action;

import fr.adrienbrault.idea.symfonyplugin.action.SymfonyContainerServiceBuilder;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;

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

    public void testActionAvailableForXmlFile() {
        myFixture.configureByText("test.xml", "");
        assertTrue(myFixture.testAction(new SymfonyContainerServiceBuilder()).isEnabledAndVisible());
    }

    public void testActionAvailableForYmlFile() {
        myFixture.configureByText("test.yml", "");
        assertTrue(myFixture.testAction(new SymfonyContainerServiceBuilder()).isEnabledAndVisible());
    }
}
