package fr.adrienbrault.idea.symfony2plugin.tests.dic.xml;

import com.intellij.ide.highlighter.XmlFileType;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.config.xml.XmlCompletionContributor
 */
public class XmlDicCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("appDevDebugProjectContainer.xml");
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes1.php"));
        myFixture.copyFileToProject("services.xml");
        myFixture.copyFileToProject("XmlDicCompletionContributorTest.env");

        myFixture.configureByText("classes.php", "<?php\n" +
            "namespace Foo\\Name;\n" +
            "class FooClass {" +
            " public function foo() " +
            "}"
        );

    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/xml/fixtures";
    }

    /**
     * @see XmlCompletionContributor
     */
    public void testServiceCompletion() {
        assertCompletionContains("service.xml",  "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "  <services>\n" +
                "      <argument type=\"service\" id=\"<caret>\"/>\n" +
                "  </services>\n" +
                "</container>"
            , "data_collector.router"
        );

        assertCompletionContains("service.xml",  "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "  <services>\n" +
                "    <service factory-service=\"<caret>\"/>" +
                "  </services>\n" +
                "</container>"
            , "data_collector.router"
        );

        assertCompletionContains("service.xml",  "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "  <services>\n" +
                "    <service parent=\"<caret>\"/>" +
                "  </services>\n" +
                "</container>"
            , "data_collector.router"
        );
    }

    /**
     * @see XmlCompletionContributor
     */
    public void testServiceCompletionForArgumentsWithInvalidTypeAttributeBuWithValidParentServiceTag() {
        assertCompletionContains("service.xml",  "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "  <services>\n" +
                "      <service><argument type=\"foobar\" id=\"<caret>\"/></service>\n" +
                "  </services>\n" +
                "</container>"
            , "data_collector.router"
        );

        assertCompletionContains("service.xml",  "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "  <services>\n" +
                "      <service><argument id=\"<caret>\"/></service>\n" +
                "  </services>\n" +
                "</container>"
            , "data_collector.router"
        );
    }

    public void testClassCompletion() {

        assertCompletionContains("service.xml",  "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "  <services>\n" +
                "      <service id=\"genemu.twig.extension.form\" class=\"<caret>\"/>\n" +
                "  </services>\n" +
                "</container>"
            , "FooClass"
        );

        assertCompletionContains("service.xml",  "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "  <services>\n" +
                "    <service factory-class=\"<caret>\"/>" +
                "  </services>\n" +
                "</container>"
            , "FooClass"
        );
    }

    public void testAutowiringType() {
        assertCompletionContains("service.xml",  "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "  <services>\n" +
                "    <service>\n" +
                "       <autowiring-type><caret></autowiring-type>" +
                "    </service>\n" +
                "  </services>\n" +
                "</container>"
            , "FooClass"
        );
    }

    public void testClassCompletionResult() {

        assertCompletionResultEquals("service.xml",
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "  <services>\n" +
                "    <service factory-class=\"FooClass<caret>\"/>" +
                "  </services>\n" +
                "</container>",
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "  <services>\n" +
                "    <service factory-class=\"Foo\\Name\\FooClass\"/>" +
                "  </services>\n" +
                "</container>"
        );

        assertCompletionResultEquals("service.xml",
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "  <services>\n" +
                "    <service factory-class=\"Foo\\Name\\<caret>\"/>" +
                "  </services>\n" +
                "</container>",
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "  <services>\n" +
                "    <service factory-class=\"Foo\\Name\\FooClass\"/>" +
                "  </services>\n" +
                "</container>"
        );

    }

    public void testFactoryClassMethodCompletionResult() {

        assertCompletionContains("service.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "  <services>\n" +
                "    <factory service=\"<caret>\"/>" +
                "  </services>\n" +
                "</container>"
            , "data_collector.router"
        );

        assertCompletionContains("service.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "  <services>\n" +
                "  <service id=\"foo.factory\" class=\"Foo\\Name\\FooClass\"/>\n" +
                "  <service id=\"foo.manager\">\n" +
                "    <factory service=\"foo.factory\" method=\"<caret>\"/>" +
                "  <service>\n" +
                "  </services>\n" +
                "</container>"
            , "foo"
        );

    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.config.xml.XmlCompletionContributor.ArgumentParameterCompletionProvider
     */
    public void testArgumentParameterCompletion() {

        assertCompletionContains("service.xml", "<services><service><argument>%<caret></argument></service></services>", "%foo.class%", "%foo_bar%");
        assertCompletionContains("service.xml", "<services><service><argument><caret></argument></service></services>", "%foo.class%", "%foo_bar%");
        assertCompletionContains("service.xml", "<services><service><argument>%<caret>%</argument></service></services>", "%foo.class%", "%foo_bar%");

        assertCompletionResultEquals("service.xml",
            "<services><service><argument>%foo_bar<caret></argument></service></services>",
            "<services><service><argument>%foo_bar%</argument></service></services>"
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.config.xml.XmlCompletionContributor.ArgumentParameterCompletionProvider
     */
    public void testEnvironmentArgumentParameterCompletion() {
        assertCompletionContains(
            "service.xml",
            "<services><service><argument>%<caret></argument></service></services>",
            "%env(FOOBAR_ENV)%"
        );
    }

    public void testServiceInstanceHighlightCompletion() {
        assertCompletionLookupContainsPresentableItem(XmlFileType.INSTANCE, "" +
                "<services>" +
                "   <service class=\"Foo\\Bar\\Car\">" +
                "       <argument type=\"service\" id=\"<caret>\"/>" +
                "   </service>" +
                "</services>",
            lookupElement -> "foo_bar_apple".equals(lookupElement.getItemText()) && lookupElement.isItemTextBold() && lookupElement.isItemTextUnderlined()
        );
    }

    public void testThatUppserCaseServiceAreInsideCompletion() {
        assertCompletionContains(XmlFileType.INSTANCE, "" +
                "<services>" +
                "   <service class=\"Foo\\Bar\\Car\">" +
                "       <argument type=\"service\" id=\"<caret>\"/>" +
                "   </service>" +
                "</services>",
            "foo_bar_car_UPPER_COMPLETION"
        );
    }

    public void testServiceCompletionOfFactoryService() {
        assertCompletionContains(XmlFileType.INSTANCE, "" +
                "<services>\n" +
                "    <service>\n" +
                "        <factory service=\"<caret>\" />\n" +
                "    </service>\n" +
                "</services>",
            "foo_bar_apple"
        );
    }
}
