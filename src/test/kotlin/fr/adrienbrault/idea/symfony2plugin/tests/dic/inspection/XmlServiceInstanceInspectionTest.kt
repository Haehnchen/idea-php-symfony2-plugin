package fr.adrienbrault.idea.symfony2plugin.tests.dic.inspection

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.inspection.XmlServiceInstanceInspection
 */
class XmlServiceInstanceInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()

        myFixture.copyFileToProject("XmlServiceInstanceInspection.php")
        myFixture.copyFileToProject("XmlServiceInstanceInspection.xml")
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/dic/inspection/fixtures"
    }

    fun testConstructorInstance() {
        assertLocalInspectionContains("test.xml", "" +
            "<services>" +
            "     <service class=\"Args\\Foo\">\n" +
            "         <argument type=\"service\" id=\"args<caret>_bar\"/>\n" +
            "     </service>" +
            "</services>",
            "Expect instance of: Args\\Foo"
        )

        assertLocalInspectionContains("test.xml", "" +
                "<services>" +
                "     <service id=\"Args\\Foo\">\n" +
                "         <argument type=\"service\" id=\"args<caret>_bar\"/>\n" +
                "     </service>" +
                "</services>",
            "Expect instance of: Args\\Foo"
        )

        assertLocalInspectionContains("test.xml", "" +
                "<services>" +
                "     <service id=\"Args\\Foo\">\n" +
                "         <argument type=\"service\" index=\"0\" id=\"args<caret>_bar\"/>\n" +
                "     </service>" +
                "</services>",
            "Expect instance of: Args\\Foo"
        )

        assertLocalInspectionContains("test.xml", "" +
                "<services>" +
                "     <service id=\"Args\\Foo\">\n" +
                "         <argument type=\"service\" key=\"\$foo\" id=\"args<caret>_bar\"/>\n" +
                "     </service>" +
                "</services>",
            "Expect instance of: Args\\Foo"
        )
    }

    fun testCallInstance() {
        assertLocalInspectionContains("test.xml", "" +
                "<services>" +
                "        <service class=\"Args\\Foo\">\n" +
                "            <call method=\"setFoo\">\n" +
                "                <argument type=\"service\" id=\"args<caret>_bar\"/>\n" +
                "            </call>\n" +
                "        </service>" +
                "</services>",
            "Expect instance of: Args\\Foo"
        )

        assertLocalInspectionContains("test.xml", "" +
                "<services>" +
                "        <service id=\"Args\\Foo\">\n" +
                "            <call method=\"setFoo\">\n" +
                "                <argument type=\"service\" id=\"args<caret>_bar\"/>\n" +
                "            </call>\n" +
                "        </service>" +
                "</services>",
            "Expect instance of: Args\\Foo"
        )

        assertLocalInspectionContains("test.xml", "" +
                "<services>" +
                "        <service class=\"Args\\Foo\">\n" +
                "            <call method=\"setFoo\">\n" +
                "                <argument type=\"service\" id=\"args_bar<caret>\"/>\n" +
                "            </call>\n" +
                "        </service>" +
                "</services>",
            "Expect instance of: Args\\Foo"
        )

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
        )
    }

    fun testCallInstanceForNamedAndIndexParameter() {
        assertLocalInspectionContains("test.xml", "" +
                "<services>" +
                "     <service id=\"Args\\Foo\">\n" +
                "         <call method=\"setFoo\">\n" +
                "             <argument type=\"service\" key=\"\$car\" id=\"args_bar<caret>\"/>\n" +
                "         </call>\n" +
                "     </service>" +
                "</services>",
            "Expect instance of: Args\\Foo"
        )

        assertLocalInspectionContains("test.xml", "" +
                "<services>" +
                "     <service id=\"Args\\Foo\">\n" +
                "         <call method=\"setFoo\">\n" +
                "             <argument type=\"service\" index=\"2\" id=\"args_bar<caret>\"/>\n" +
                "         </call>\n" +
                "     </service>" +
                "</services>",
            "Expect instance of: Args\\Foo"
        )
    }
}
