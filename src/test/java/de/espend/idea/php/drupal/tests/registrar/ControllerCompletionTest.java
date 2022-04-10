package de.espend.idea.php.drupal.tests.registrar;

import de.espend.idea.php.drupal.tests.DrupalLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see de.espend.idea.php.drupal.registrar.ControllerCompletion
 */
public class ControllerCompletionTest extends DrupalLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
    }

    protected String getTestDataPath() {
        return "src/test/java/de/espend/idea/php/drupal/tests/registrar/fixtures";
    }

    public void testThatEntityFormCompletesAndNavigates() {
        assertCompletionContains(YAMLFileType.YML, "" +
                "config.import_full:\n" +
                "  defaults:\n" +
                "    _controller: '<caret>'",
            "\\Drupal\\contact\\Controller\\ContactController::foo"
        );

        assertCompletionNotContains(YAMLFileType.YML, "" +
                "config.import_full:\n" +
                "  defaults:\n" +
                "    _controller: '<caret>'",
            "\\Drupal\\contact\\Controller\\ContactController::privateBar"
        );

        assertCompletionNotContains(YAMLFileType.YML, "" +
                "config.import_full:\n" +
                "  defaults:\n" +
                "    _controller: '<caret>'",
            "\\Drupal\\contact\\Controller\\ContactController::__construct"
        );
    }

}
