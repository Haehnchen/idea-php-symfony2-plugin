package de.espend.idea.php.drupal.tests.registrar;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import de.espend.idea.php.drupal.tests.DrupalLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see de.espend.idea.php.drupal.registrar.YamlPermissionGotoCompletion
 */
public class YamlEntityFormGotoCompletionTest extends DrupalLightCodeInsightFixtureTestCase {

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
                "    _entity_form: '<caret>'",
            "contact_form"
        );

        assertNavigationMatch(YAMLFileType.YML, "" +
                "config.import_full:\n" +
                "  defaults:\n" +
                "    _entity_form: 'contact_<caret>form'",
            PlatformPatterns.psiElement(PhpClass.class)
        );
    }

}
