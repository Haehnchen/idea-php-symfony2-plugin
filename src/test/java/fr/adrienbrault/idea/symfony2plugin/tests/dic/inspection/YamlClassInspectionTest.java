package fr.adrienbrault.idea.symfony2plugin.tests.dic.inspection;

import fr.adrienbrault.idea.symfony2plugin.dic.inspection.YamlClassInspection;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

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
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/inspection/fixtures";
    }

    public void testInspectionForClass() {
        assertLocalInspectionContains("services.yml", "services:\n  class: Args\\Fo<caret>oBar", YamlClassInspection.MESSAGE_MISSING_CLASS);
        assertLocalInspectionContains("services.yml", "services:\n  class: 'Args\\Fo<caret>oBar'", YamlClassInspection.MESSAGE_MISSING_CLASS);
        assertLocalInspectionContains("services.yml", "services:\n  class: \"Args\\Fo<caret>oBar\"", YamlClassInspection.MESSAGE_MISSING_CLASS);
        assertLocalInspectionContains("services.yml", "services:\n  factory_class: Args\\Fo<caret>oBar", YamlClassInspection.MESSAGE_MISSING_CLASS);
        assertLocalInspectionNotContains("services.yml", "services:\n  factory_class: Args\\Fo<caret>o", YamlClassInspection.MESSAGE_MISSING_CLASS);

        assertLocalInspectionContains("services.yml", "parameters:\n  foo.class: Args\\Fo<caret>oBar", YamlClassInspection.MESSAGE_MISSING_CLASS);
        assertLocalInspectionContains("services.yml", "parameters:\n  foo.class: 'Args\\Fo<caret>oBar'", YamlClassInspection.MESSAGE_MISSING_CLASS);
        assertLocalInspectionContains("services.yml", "parameters:\n  foo.class: \"Args\\Fo<caret>oBar\"", YamlClassInspection.MESSAGE_MISSING_CLASS);
        assertLocalInspectionNotContains("services.yml", "parameters:\n  foo.class: Args\\Fo<caret>o", YamlClassInspection.MESSAGE_MISSING_CLASS);

        assertLocalInspectionContains("services.yml", "services:\n  class: Args\\Fo<caret>O", YamlClassInspection.MESSAGE_WRONG_CASING);
        assertLocalInspectionContains("services.yml", "services:\n  class: 'Args\\Fo<caret>O'", YamlClassInspection.MESSAGE_WRONG_CASING);
        assertLocalInspectionContains("services.yml", "services:\n  class: \"Args\\Fo<caret>O\"", YamlClassInspection.MESSAGE_WRONG_CASING);
        assertLocalInspectionContains("services.yml", "services:\n  factory_class: Args\\Fo<caret>O", YamlClassInspection.MESSAGE_WRONG_CASING);
        assertLocalInspectionNotContains("services.yml", "services:\n  factory_class: Args\\Fo<caret>o", YamlClassInspection.MESSAGE_WRONG_CASING);

        assertLocalInspectionContains("services.yml", "parameters:\n  foo.class: Args\\Fo<caret>O", YamlClassInspection.MESSAGE_WRONG_CASING);
        assertLocalInspectionContains("services.yml", "parameters:\n  foo.class: 'Args\\Fo<caret>O'", YamlClassInspection.MESSAGE_WRONG_CASING);
        assertLocalInspectionContains("services.yml", "parameters:\n  foo.class: \"Args\\Fo<caret>O\"", YamlClassInspection.MESSAGE_WRONG_CASING);
        assertLocalInspectionNotContains("services.yml", "parameters:\n  foo.class: Args\\Fo<caret>o", YamlClassInspection.MESSAGE_WRONG_CASING);
    }
}
