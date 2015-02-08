package fr.adrienbrault.idea.symfony2plugin.tests.dic.xml;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.config.xml.XmlCompletionContributor
 */
public class XmlDicInspectionsTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("appDevDebugProjectContainer.xml");

        myFixture.configureByText("deprecated.php", "<?php\n" +
            "namespace Foo\\Name;\n" +
            "\n" +
            "/**\n" +
            " * @deprecated\n" +
            " */\n" +
            "class DeprecatedClass {}"
        );

    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("..").getFile()).getAbsolutePath();
    }

    public void testThatUnknownServiceIsHighlighted() {

        myFixture.configureByText("service.xml","<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<container>\n" +
                "  <services>\n" +
                "      <argument type=\"service\" id=\"<error descr=\"Cannot resolve symbol 'foo.service'\">foo.service</error>\"/>\n" +
                "  </services>\n" +
                "</container>"
        );

        myFixture.checkHighlighting();
    }


}
