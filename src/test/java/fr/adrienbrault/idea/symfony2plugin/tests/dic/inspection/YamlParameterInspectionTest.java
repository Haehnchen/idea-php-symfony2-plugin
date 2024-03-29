package fr.adrienbrault.idea.symfony2plugin.tests.dic.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.inspection.YamlParameterInspection
 */
public class YamlParameterInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("YamlParameterInspection.xml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/inspection/fixtures";
    }

    public void testParameterInspection() {
        assertLocalInspectionContains("services.yml", "services:\n   %foo_<caret>missing%", "Symfony: Missing Parameter");
        assertLocalInspectionNotContains("services.yml", "services:\n   %foo_p<caret>arameter%", "Symfony: Missing Parameter");

        assertLocalInspectionContains("services.yml", "services:\n   %Foo_<caret>missing%", "Symfony: Missing Parameter");
        assertLocalInspectionNotContains("services.yml", "services:\n   %Foo_p<caret>arameter%", "Symfony: Missing Parameter");

        assertLocalInspectionContains("services.yml", "services:\n   [ '%Foo_<caret>missing%' ]\n", "Symfony: Missing Parameter");
        assertLocalInspectionContains("services.yml", "services:\n   [ \"%Foo_<caret>missing%\" ]\n", "Symfony: Missing Parameter");
        assertLocalInspectionNotContains("services.yml", "services:\n   %kernel.root_dir%/../we<caret>b/%webpath_modelmasks%", "Symfony: Missing Parameter");
    }

    public void testParameterInspectionForEnvMustNotAnnotationAsMissing() {
        assertLocalInspectionNotContains("services.yml", "services:\n   %env(FO<caret>O)%", "Symfony: Missing Parameter");
        assertLocalInspectionNotContains("services.yml", "services:\n   '%env(FO<caret>O)%'", "Symfony: Missing Parameter");
        assertLocalInspectionNotContains("services.yml", "services:\n   \"%env(FO<caret>O)%\"", "Symfony: Missing Parameter");
        assertLocalInspectionNotContains("services.yml", "services:\n   '%ENV(FO<caret>O)%'", "Symfony: Missing Parameter");
    }
}
