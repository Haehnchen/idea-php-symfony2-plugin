package fr.adrienbrault.idea.symfony2plugin.tests.dic.inspection;

import fr.adrienbrault.idea.symfony2plugin.dic.inspection.ServiceNamedArgumentExistsInspection;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceNamedArgumentExistsInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("services.xml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/inspection/fixtures";
    }

    public void testMissingArgumentForYaml() {
        assertLocalInspectionContains("foo.yml",
            "Foobar\\NamedArgument:\n" +
                "        arguments:\n" +
                "            $foo<caret>bar1: ~",
            ServiceNamedArgumentExistsInspection.INSPECTION_MESSAGE
        );

        assertLocalInspectionNotContains("foo.yml",
            "Foobar\\UnknownClassNamedArgument:\n" +
                "        arguments:\n" +
                "            $foo<caret>bar: ~",
            ServiceNamedArgumentExistsInspection.INSPECTION_MESSAGE
        );
    }

    public void testMissingArgumentForFactoryServiceIsNotTriggeredYaml() {
        assertLocalInspectionNotContains("foo.yml",
            "Foobar\\NamedArgument:\n" +
                "        factory: ~\n" +
                "        arguments:\n" +
                "            $foo<caret>bar1: ~",
            ServiceNamedArgumentExistsInspection.INSPECTION_MESSAGE
        );
    }
}
