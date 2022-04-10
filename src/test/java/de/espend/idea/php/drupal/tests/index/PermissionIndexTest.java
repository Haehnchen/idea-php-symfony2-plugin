package de.espend.idea.php.drupal.tests.index;

import de.espend.idea.php.drupal.index.PermissionIndex;
import de.espend.idea.php.drupal.tests.DrupalLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see PermissionIndex
 */
public class PermissionIndexTest extends DrupalLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("config.permissions.yml");
    }

    protected String getTestDataPath() {
        return "src/test/java/de/espend/idea/php/drupal/tests/index/fixtures";
    }

    public void testThatMappingValueIsInIndex() {
        assertIndexContains(PermissionIndex.KEY, "import configuration");
    }
}
