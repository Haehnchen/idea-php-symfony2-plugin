package fr.adrienbrault.idea.symfony2plugin.tests.config.xml.inspection;

import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.config.xml.inspection.XmlServiceArgumentInspection
 */
public class XmlServiceArgumentInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testMissingArgumentProvidesInspection() {
        assertLocalInspectionContains(
            "services.xml",
            createContainer("<serv<caret>ice class=\"Foo\\Bar\"/>"),
            "Missing argument"
        );

        assertLocalInspectionContains(
            "services.xml",
            createContainer("<serv<caret>ice class=\"Foo\\Bar\"><argument>foo</argument></service>"),
            "Missing argument"
        );
    }

    public void testThatAllParametersAreGiven() {
        assertLocalInspectionIsEmpty(
            "services.xml",
            createContainer("<serv<caret>ice class=\"Foo\\Bar\"><argument>foo</argument><argument>foo</argument></service>")
        );
    }

    public void testThatAllParametersAreGivenWithLastOneOptional() {
        assertLocalInspectionIsEmpty(
            "services.xml",
            createContainer("<serv<caret>ice class=\"Foo\\Apple\"><argument>foo</argument></service>")
        );
    }

    public void testThatNotSupportServiceAttributeNotProvidesInspection() {
        for (String s : ServiceActionUtil.INVALID_ARGUMENT_ATTRIBUTES) {
            assertLocalInspectionIsEmpty(
                "services.xml",
                createContainer("<serv<caret>ice " + s +"=\"foo\" class=\"Foo\\Bar\"/>")
            );
        }
    }

    public void testThatFactoryServiceOfSymfony26NotProvidesInspection() {
        for (String s : ServiceActionUtil.INVALID_ARGUMENT_ATTRIBUTES) {
            assertLocalInspectionIsEmpty(
                "services.xml",
                createContainer("<serv<caret>ice " + s +"=\"foo\" class=\"Foo\\Bar\"><factory/></service>")
            );
        }
    }

    private String createContainer(String serviceDefinition) {
        return "<container><services>" + serviceDefinition + "</services></container>";
    }
}
