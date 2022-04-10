package de.espend.idea.php.drupal.tests.registrar;

import de.espend.idea.php.drupal.tests.DrupalLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see de.espend.idea.php.drupal.registrar.YamlRouteKeyCompletion
 */
public class YamlRouteKeyCompletionTest extends DrupalLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("search.menu.yml");
    }

    protected String getTestDataPath() {
        return "src/test/java/de/espend/idea/php/drupal/tests/registrar/fixtures";
    }

    public void testCompletion() {
        assertCompletionContains("foo.routing.yml", "" +
            "config.import_full:\n" +
            "  defaults:" +
            "    <caret>: foo" +
            "_entity_form"
        );

        assertCompletionContains("foo.routing.yml", "" +
            "config.import_full:\n" +
            "  requirements:" +
            "    <caret>: foo" +
            "_entity_access"
        );
    }
}
