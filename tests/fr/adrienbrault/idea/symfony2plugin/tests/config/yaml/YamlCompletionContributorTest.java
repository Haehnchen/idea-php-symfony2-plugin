package fr.adrienbrault.idea.symfony2plugin.tests.config.yaml;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureByText("config_foo.yml", "");
    }

    public void testResourcesInsideSameDirectoryProvidesCompletion() {
        assertCompletionContains("config.yml", "imports:\n" +
            "    - { resource: <caret> }",
            "config_foo.yml"
        );
    }
}
