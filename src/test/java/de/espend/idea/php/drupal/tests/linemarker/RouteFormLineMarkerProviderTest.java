package de.espend.idea.php.drupal.tests.linemarker;

import de.espend.idea.php.drupal.tests.DrupalLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see de.espend.idea.php.drupal.linemarker.RouteFormLineMarkerProvider
 */
public class RouteFormLineMarkerProviderTest extends DrupalLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
    }

    protected String getTestDataPath() {
        return "src/test/java/de/espend/idea/php/drupal/tests/linemarker/fixtures";
    }

    public void testLinemarkerForForm() {
        assertLineMarker(myFixture.configureByText(YAMLFileType.YML, "" +
            "config.export_full:\n" +
            "  defaults:\n" +
            "    _form: '\\Foo\\FooBar'"
        ), new LineMarker.ToolTipEqualsAssert("Navigate to form"));
    }

    public void testLinemarkerForEntityForm() {
        assertLineMarker(myFixture.configureByText(YAMLFileType.YML, "" +
            "config.export_full:\n" +
            "  defaults:\n" +
            "    _entity_form: 'contact_form'"
        ), new LineMarker.ToolTipEqualsAssert("Navigate to form"));
    }
}
