package fr.adrienbrault.idea.symfony2plugin.tests.codeInspection.service;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.codeInspection.service.TaggedExtendsInterfaceClassInspection
 */
public class TaggedExtendsInterfaceClassInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/codeInspection/service/fixtures";
    }

    public void testThatKnownTagsShouldInspectionForMissingServiceClassImplementationsOfYaml() {
        assertLocalInspectionContains("services.yml", "services:\n" +
            "    foo:\n" +
            "        class: Tag\\Instance<caret>Check\\EmptyClass\n" +
            "        tags:\n" +
            "            -  { name: twig.extension }",
            "Class needs to implement 'Twig_ExtensionInterface' for tag 'twig.extension'"
        );
    }

    public void testThatKnownTagsShouldInspectionForMissingServiceClassImplementationsForClassAsIsOfYaml() {
        assertLocalInspectionContains("services.yml", "services:\n" +
                "    Tag\\Instance<caret>Check\\EmptyClass:\n" +
                "        tags:\n" +
                "            -  { name: twig.extension }",
            "Class needs to implement 'Twig_ExtensionInterface' for tag 'twig.extension'"
        );
    }

    public void testThatKnownTagsShouldInspectionForMissingServiceClassImplementationsOfXml() {
        assertLocalInspectionContains(
            "services.xml",
                "<?xml version=\"1.0\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "        <service id=\"test\" class=\"Tag\\Instance<caret>Check\\EmptyClass\">\n" +
                "             <tag name=\"twig.extension\"/>" +
                "        </service>\n" +
                "    </services>\n" +
                "</container>\n",
            "Class needs to implement 'Twig_ExtensionInterface' for tag 'twig.extension'"
        );
    }

    public void testThatKnownTagsShouldInspectionForMissingServiceClassImplementationsForClassAsIsOfYamlOfYml() {
        assertLocalInspectionContains(
            "services.xml",
            "<?xml version=\"1.0\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "        <service id=\"Tag\\Instance<caret>Check\\EmptyClass\">\n" +
                "             <tag name=\"twig.extension\"/>" +
                "        </service>\n" +
                "    </services>\n" +
                "</container>\n",
            "Class needs to implement 'Twig_ExtensionInterface' for tag 'twig.extension'"
        );
    }
}
