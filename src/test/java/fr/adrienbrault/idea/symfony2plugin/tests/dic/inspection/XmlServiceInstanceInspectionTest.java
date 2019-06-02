package fr.adrienbrault.idea.symfony2plugin.tests.dic.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.inspection.XmlServiceInstanceInspection
 */
public class XmlServiceInstanceInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("XmlServiceInstanceInspection.php");
        myFixture.copyFileToProject("XmlServiceInstanceInspection.xml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/inspection/fixtures";
    }

    public void testConstructorInstance() {
        assertLocalInspectionContains("test.xml", "" +
            "<services>" +
            "     <service class=\"Args\\Foo\">\n" +
            "         <argument type=\"service\" id=\"args<caret>_bar\"/>\n" +
            "     </service>" +
            "</services>",
            "Expect instance of: Args\\Foo"
        );

        assertLocalInspectionContains("test.xml", "" +
                "<services>" +
                "     <service id=\"Args\\Foo\">\n" +
                "         <argument type=\"service\" id=\"args<caret>_bar\"/>\n" +
                "     </service>" +
                "</services>",
            "Expect instance of: Args\\Foo"
        );

        assertLocalInspectionContains("test.xml", "" +
                "<services>" +
                "     <service id=\"Args\\Foo\">\n" +
                "         <argument type=\"service\" index=\"0\" id=\"args<caret>_bar\"/>\n" +
                "     </service>" +
                "</services>",
            "Expect instance of: Args\\Foo"
        );

        assertLocalInspectionContains("test.xml", "" +
                "<services>" +
                "     <service id=\"Args\\Foo\">\n" +
                "         <argument type=\"service\" key=\"$foo\" id=\"args<caret>_bar\"/>\n" +
                "     </service>" +
                "</services>",
            "Expect instance of: Args\\Foo"
        );
    }

    public void testCallInstance() {
        assertLocalInspectionContains("test.xml", "" +
                "<services>" +
                "        <service class=\"Args\\Foo\">\n" +
                "            <call method=\"setFoo\">\n" +
                "                <argument type=\"service\" id=\"args<caret>_bar\"/>\n" +
                "            </call>\n" +
                "        </service>" +
                "</services>",
            "Expect instance of: Args\\Foo"
        );

        assertLocalInspectionContains("test.xml", "" +
                "<services>" +
                "        <service id=\"Args\\Foo\">\n" +
                "            <call method=\"setFoo\">\n" +
                "                <argument type=\"service\" id=\"args<caret>_bar\"/>\n" +
                "            </call>\n" +
                "        </service>" +
                "</services>",
            "Expect instance of: Args\\Foo"
        );

        assertLocalInspectionContains("test.xml", "" +
                "<services>" +
                "        <service class=\"Args\\Foo\">\n" +
                "            <call method=\"setFoo\">\n" +
                "                <argument type=\"service\" id=\"args_bar<caret>\"/>\n" +
                "            </call>\n" +
                "        </service>" +
                "</services>",
            "Expect instance of: Args\\Foo"
        );

        assertLocalInspectionContains("test.xml", "" +
                "<services>" +
                "     <service class=\"Args\\Foo\">\n" +
                "         <call method=\"setFoo\">\n" +
                "             <argument/>\n" +
                "             <argument/>\n" +
                "             <argument type=\"service\" id=\"args_bar<caret>\"/>\n" +
                "         </call>\n" +
                "     </service>" +
                "</services>",
            "Expect instance of: Args\\Foo"
        );
    }

    public void testCallInstanceForNamedAndIndexParameter() {
        assertLocalInspectionContains("test.xml", "" +
                "<services>" +
                "     <service id=\"Args\\Foo\">\n" +
                "         <call method=\"setFoo\">\n" +
                "             <argument type=\"service\" key=\"$car\" id=\"args_bar<caret>\"/>\n" +
                "         </call>\n" +
                "     </service>" +
                "</services>",
            "Expect instance of: Args\\Foo"
        );

        assertLocalInspectionContains("test.xml", "" +
                "<services>" +
                "     <service id=\"Args\\Foo\">\n" +
                "         <call method=\"setFoo\">\n" +
                "             <argument type=\"service\" index=\"2\" id=\"args_bar<caret>\"/>\n" +
                "         </call>\n" +
                "     </service>" +
                "</services>",
            "Expect instance of: Args\\Foo"
        );
    }
}
