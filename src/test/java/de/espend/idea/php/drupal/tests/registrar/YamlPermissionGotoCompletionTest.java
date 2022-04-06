package de.espend.idea.php.drupal.tests.registrar;

import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import de.espend.idea.php.drupal.tests.DrupalLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see de.espend.idea.php.drupal.registrar.YamlPermissionGotoCompletion
 */
public class YamlPermissionGotoCompletionTest extends DrupalLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("config.permissions.yml");
    }

    protected String getTestDataPath() {
        return "src/test/java/de/espend/idea/php/drupal/tests/registrar/fixtures";
    }

    public void testThatRoutePermissionCompletesAndNavigates() {
        assertCompletionContains(YAMLFileType.YML, "" +
                "config.import_full:\n" +
                "  requirements:\n" +
                "    _permission: '<caret>'",
            "synchronize configuration"
        );

        assertNavigationMatch(YAMLFileType.YML, "" +
                "config.import_full:\n" +
                "  requirements:\n" +
                "    _permission: 'synchronize<caret> configuration'",
            PlatformPatterns.psiElement(YAMLKeyValue.class).with(new PatternCondition<YAMLKeyValue>("key") {
                @Override
                public boolean accepts(@NotNull YAMLKeyValue yamlKeyValue, ProcessingContext processingContext) {
                    return "synchronize configuration".equals(yamlKeyValue.getKeyText());
                }
            })
        );
    }

}
