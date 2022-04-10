package de.espend.idea.php.drupal.tests.registrar;

import de.espend.idea.php.drupal.tests.DrupalLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see de.espend.idea.php.drupal.registrar.YamlMenuGotoCompletion
 */
public class YamlMenuGotoCompletionTest extends DrupalLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("search.menu.yml");
    }

    protected String getTestDataPath() {
        return "src/test/java/de/espend/idea/php/drupal/tests/registrar/fixtures";
    }

    public void testCompletesAndNavigates() {
        assertCompletionContains("foo.menu.yml", "" +
            "config.import_full:\n" +
            "  parent: '<caret>'" +
            "search.view"
        );

        assertCompletionContains("foo.menu.yml", "" +
            "config.import_full:\n" +
            "  parent: <caret>" +
            "search.view"
        );
    }

    public void testMenuKeyCompletion() {
        assertCompletionContains("foo.menu.yml", "" +
            "config.import_full:\n" +
            "  <caret>: foo" +
            "route_name"
        );
    }
}
