package fr.adrienbrault.idea.symfony2plugin.tests.config.yaml;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlGoToKnownDeclarationHandlerTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureByText("config_foo.yml", "");
    }

    public void testResourcesInsideSameDirectoryProvidesNavigation() {
        assertNavigationContainsFile(YAMLFileType.YML, "imports:\n" +
                "    - { resource: config_<caret>foo.yml }",
            "config_foo.yml"
        );

        assertNavigationContainsFile(YAMLFileType.YML, "imports:\n" +
                "    - { resource: 'config_<caret>foo.yml' }",
            "config_foo.yml"
        );

        assertNavigationContainsFile(YAMLFileType.YML, "imports:\n" +
                "    - { resource: \"config_<caret>foo.yml\" }",
            "config_foo.yml"
        );
    }
}
