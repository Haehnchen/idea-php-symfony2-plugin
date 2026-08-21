package fr.adrienbrault.idea.symfony2plugin.tests.routing.inspection

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import java.util.ArrayList

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.routing.inspection.RouteSettingDeprecatedInspection
 */
class RouteSettingDeprecatedInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testXmlRequirementsAreDeprecated() {
        assertLocalInspectionContains("routing.xml", "" +
            "<route>\n" +
            "<requirement key=\"_me<caret>thod\">POST|PUT</requirement>\n" +
            "</route>:\n",
            "The '_method' requirement is deprecated"
        )

        assertLocalInspectionContains("routing.xml", "" +
            "<route>\n" +
            "<requirement key=\"_sch<caret>eme\">https</requirement>\n" +
            "</route>:\n",
            "The '_scheme' requirement is deprecated"
        )
    }

    fun testXmlRoutePatternAreDeprecated() {
        assertLocalInspectionContains("routing.xml", "" +
            "<route pat<caret>tern=\"foo\"/>\n",
            "Pattern is deprecated; use path instead"
        )

        assertLocalInspectionNotContains("routing.xml", "" +
            "<route pattern=\"f<caret>oo\"/>\n",
            "Pattern is deprecated; use path instead"
        )
    }

    fun testYmlRoutePatternAreDeprecated() {
        assertLocalInspectionContains("routing.yml", "" +
            "foo:\n" +
            "  pa<caret>ttern: foo",
            "Pattern is deprecated; use path instead"
        )
    }

    fun testYmlRequirementsAreDeprecated() {
        val providers: MutableCollection<Array<String>> = ArrayList()
        providers.add(arrayOf("_m<caret>ethod: foo", "The '_method' requirement is deprecated"))
        providers.add(arrayOf("'_m<caret>ethod': foo", "The '_method' requirement is deprecated"))
        providers.add(arrayOf("\"_m<caret>ethod\": foo", "The '_method' requirement is deprecated"))
        providers.add(arrayOf("'_sch<caret>eme': foo", "The '_scheme' requirement is deprecated"))

        for (s in providers) {
            assertLocalInspectionContains("routing.yml", "" +
                "foo:\n" +
                "   requirements: { " + s[0] + " }",
                s[1]
            )

            assertLocalInspectionContains("routing.yml", "" +
                "foo:\n" +
                "   requirements:\n" +
                "      " + s[0] + "\n",
                s[1]
            )
        }

        assertLocalInspectionNotContains("routing.yml", "" +
            "foo:\n" +
            "   bar: { _m<caret>ethod: foo }",
            "The '_method' requirement is deprecated"
        )
    }
}
