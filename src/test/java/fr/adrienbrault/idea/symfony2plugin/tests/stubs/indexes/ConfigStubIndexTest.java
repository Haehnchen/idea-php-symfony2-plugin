package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ConfigStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ConfigStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("twig_component.yaml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/stubs/indexes/fixtures";
    }

    public void testIndexingForConfigValues() {
        assertIndexContains(ConfigStubIndex.KEY, "anonymous_template_directory");

        assertIndexContainsKeyWithValue(
            ConfigStubIndex.KEY,
            "anonymous_template_directory",
            value -> "anonymous_template_directory".equals(value.getName()) && value.getValues().contains("components/")
        );

        assertIndexContains(ConfigStubIndex.KEY, "twig_component_defaults");

        assertIndexContainsKeyWithValue(
            ConfigStubIndex.KEY,
            "twig_component_defaults",
            value -> value.getConfigs().get("App\\Twig\\Components2\\").get("template_directory").contains("components")
        );
    }
}
