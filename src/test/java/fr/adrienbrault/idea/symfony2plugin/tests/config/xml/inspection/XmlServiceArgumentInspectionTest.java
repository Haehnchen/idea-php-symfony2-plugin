package fr.adrienbrault.idea.symfony2plugin.tests.config.xml.inspection;

import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfony2plugin.config.xml.inspection.XmlServiceArgumentInspection;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

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
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/config/xml/inspection/fixtures";
    }

    public void testMissingArgumentProvidesInspection() {
        assertLocalInspectionContains(XmlServiceArgumentInspection.class,
            "services.xml",
            createContainer("<serv<caret>ice class=\"Foo\\Bar\"/>"),
            "Missing argument"
        );

        assertLocalInspectionContains(XmlServiceArgumentInspection.class,
            "services.xml",
            createContainer("<serv<caret>ice class=\"Foo\\Bar\"><argument>foo</argument></service>"),
            "Missing argument"
        );
    }

    public void testMissingArgumentNotProvidesInspection() {
        assertLocalInspectionNotContains(XmlServiceArgumentInspection.class,
            "services.xml",
            "<foo<caret>bar class=\"Foo\\Bar\"/>",
            "Missing argument"
        );

        assertLocalInspectionNotContains(XmlServiceArgumentInspection.class,
            "services.html.twig",
            "<serv<caret>ice class=\"Foo\\Bar\"/>",
            "Missing argument"
        );
    }

    public void testThatAllParametersAreGiven() {
        assertLocalInspectionNotContains(XmlServiceArgumentInspection.class,
            "services.xml",
            createContainer("<serv<caret>ice class=\"Foo\\Bar\"><argument>foo</argument><argument>foo</argument></service>"),
            "Missing argument"
        );
    }

    public void testThatAllParametersAreGivenWithLastOneOptional() {
        assertLocalInspectionNotContains(XmlServiceArgumentInspection.class,
            "services.xml",
            createContainer("<serv<caret>ice class=\"Foo\\Apple\"><argument>foo</argument></service>"),
            "Missing argument"
        );
    }

    public void testThatNotSupportServiceAttributeNotProvidesInspection() {
        for (String s : ServiceActionUtil.INVALID_ARGUMENT_ATTRIBUTES) {
            assertLocalInspectionNotContains(XmlServiceArgumentInspection.class,
                "services.xml",
                createContainer("<serv<caret>ice " + s +"=\"foo\" class=\"Foo\\Bar\"/>"),
                "Missing argument"
            );
        }
    }

    public void testThatServiceResourceMustNotProvideInspection() {
        assertLocalInspectionNotContains(XmlServiceArgumentInspection.class,
            "services.xml",
            createContainer("<serv<caret>ice resource=\"foo\" class=\"Foo\\Bar\"/>"),
            "Missing argument"
        );
    }

    public void testThatServiceFactoryServiceMustNotProvideInspection() {
        assertLocalInspectionNotContains(XmlServiceArgumentInspection.class,
            "services.xml",
            createContainer("<serv<caret>ice factory-service=\"foo\" class=\"Foo\\Bar\"/>"),
            "Missing argument"
        );
    }

    public void testThatFactoryServiceOfSymfony26NotProvidesInspection() {
        for (String s : ServiceActionUtil.INVALID_ARGUMENT_ATTRIBUTES) {
            assertLocalInspectionNotContains(XmlServiceArgumentInspection.class,
                "services.xml",
                createContainer("<serv<caret>ice " + s +"=\"foo\" class=\"Foo\\Bar\"><factory/></service>"),
                "Missing argument"
            );
        }
    }

    public void testThatDefaultValueMustNotProvideInspection() {
        assertLocalInspectionNotContains(XmlServiceArgumentInspection.class,
            "services.xml",
            createContainer("<defaults autowire=\"true\" /><serv<caret>ice class=\"Foo\\Bar\"/>"),
            "Missing argument"
        );

        assertLocalInspectionContains(XmlServiceArgumentInspection.class,
            "services.xml",
            createContainer("<defaults autowire=\"false\" /><serv<caret>ice class=\"Foo\\Bar\"/>"),
            "Missing argument"
        );

        assertLocalInspectionContains(XmlServiceArgumentInspection.class,
            "services.xml",
            createContainer("<defaults autowire=\"true\" /><serv<caret>ice autowire=\"false\" class=\"Foo\\Bar\"/>"),
            "Missing argument"
        );
    }

    public void testThatServiceShortcutOnIdAttributeIsProvidesInspection() {
        assertLocalInspectionContains(XmlServiceArgumentInspection.class,
            "services.xml",
            createContainer("<serv<caret>ice id=\"Foo\\Bar\"/>"),
            "Missing argument"
        );
    }

    private String createContainer(String serviceDefinition) {
        return "<container><services>" + serviceDefinition + "</services></container>";
    }
}
