package fr.adrienbrault.idea.symfony2plugin.tests.dic.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.inspection.YamlClassInspection
 */
public class YamlClassInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("YamlClassInspection.php");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testInspectionForClass() {
        assertLocalInspectionContains("services.yml", "services:\n  class: Args\\Fo<caret>oBar", "Missing Class");
        assertLocalInspectionContains("services.yml", "services:\n  class: 'Args\\Fo<caret>oBar'", "Missing Class");
        assertLocalInspectionContains("services.yml", "services:\n  class: \"Args\\Fo<caret>oBar\"", "Missing Class");
        assertLocalInspectionContains("services.yml", "services:\n  factory_class: Args\\Fo<caret>oBar", "Missing Class");
        assertLocalInspectionNotContains("services.yml", "services:\n  factory_class: Args\\Fo<caret>o", "Missing Class");

        assertLocalInspectionContains("services.yml", "parameters:\n  foo.class: Args\\Fo<caret>oBar", "Missing Class");
        assertLocalInspectionContains("services.yml", "parameters:\n  foo.class: 'Args\\Fo<caret>oBar'", "Missing Class");
        assertLocalInspectionContains("services.yml", "parameters:\n  foo.class: \"Args\\Fo<caret>oBar\"", "Missing Class");
        assertLocalInspectionNotContains("services.yml", "parameters:\n  foo.class: Args\\Fo<caret>o", "Missing Class");
    }
}
