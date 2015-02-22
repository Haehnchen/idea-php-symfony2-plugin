package fr.adrienbrault.idea.symfony2plugin.tests.dic.xml;

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

        myFixture.configureByText("classes.php", "<?php\n" +
            "namespace Foo\\Name;\n" +
            "class FooClass {" +
            " public function foo() " +
            "}"
        );

    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("..").getFile()).getAbsolutePath();
    }

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

}
