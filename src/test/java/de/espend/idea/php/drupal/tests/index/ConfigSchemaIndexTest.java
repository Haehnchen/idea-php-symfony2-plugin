package de.espend.idea.php.drupal.tests.index;

import de.espend.idea.php.drupal.index.ConfigSchemaIndex;
import de.espend.idea.php.drupal.tests.DrupalLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ConfigSchemaIndexTest extends DrupalLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("action.schema.yml");
    }

    protected String getTestDataPath() {
        return "src/test/java/de/espend/idea/php/drupal/tests/index/fixtures";
    }

    public void testThatMappingValueIsInIndex() {
        assertIndexContains(ConfigSchemaIndex.KEY, "action.settings");

        assertIndexContainsKeyWithValue(ConfigSchemaIndex.KEY, "action.settings", value ->
            value.contains("recursion_limit")
        );
    }
}
