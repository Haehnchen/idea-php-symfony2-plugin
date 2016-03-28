package fr.adrienbrault.idea.symfony2plugin.tests.routing.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.routing.inspection.RouteSettingDeprecatedInspection
 */
public class RouteSettingDeprecatedInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testXmlRequirementsAreDeprecated() {
        assertLocalInspectionContains("routing.xml", "" +
                "<route>\n" +
                "<requirement key=\"_me<caret>thod\">POST|PUT</requirement>\n" +
                "</route>:\n",
            "The '_method' requirement is deprecated"
        );

        assertLocalInspectionContains("routing.xml", "" +
                "<route>\n" +
                "<requirement key=\"_sch<caret>eme\">https</requirement>\n" +
                "</route>:\n",
            "The '_scheme' requirement is deprecated"
        );
    }

    public void testXmlRoutePatternAreDeprecated() {
        assertLocalInspectionContains("routing.xml", "" +
                "<route pat<caret>tern=\"foo\"/>\n",
            "Pattern is deprecated; use path instead"
        );

        assertLocalInspectionContainsNotContains("routing.xml", "" +
                "<route pattern=\"f<caret>oo\"/>\n",
            "Pattern is deprecated; use path instead"
        );
    }

    public void testYmlRoutePatternAreDeprecated() {
        assertLocalInspectionContains("routing.yml", "" +
                "foo:\n" +
                "  pa<caret>ttern: foo",
            "Pattern is deprecated; use path instead"
        );
    }

    public void testYmlRequirementsAreDeprecated() {

        Collection<String[]> providers = new ArrayList<String[]>() {{
            add(new String[] {"_m<caret>ethod: foo", "The '_method' requirement is deprecated"});
            add(new String[] {"'_m<caret>ethod': foo", "The '_method' requirement is deprecated"});
            add(new String[] {"\"_m<caret>ethod\": foo", "The '_method' requirement is deprecated"});
            add(new String[] {"'_sch<caret>eme': foo", "The '_scheme' requirement is deprecated"});
        }};

        for (String[] s : providers) {
            assertLocalInspectionContains("routing.yml", "" +
                    "foo:\n" +
                    "   requirements: { " + s[0] + " }",
                s[1]
            );

            assertLocalInspectionContains("routing.yml", "" +
                    "foo:\n" +
                    "   requirements:\n" +
                    "      " + s[0] + "\n",
                s[1]
            );
        }

        assertLocalInspectionContainsNotContains("routing.yml", "" +
                "foo:\n" +
                "   bar: { _m<caret>ethod: foo }",
            "The '_method' requirement is deprecated"
        );
    }
}
