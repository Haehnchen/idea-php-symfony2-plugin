package fr.adrienbrault.idea.symfony2plugin.tests.dic.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.inspection.CaseSensitivityServiceInspection
 */
public class CaseSensitivityServiceInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testCaseSensitivityForXmlFiles() {
        assertLocalInspectionContains("service.xml",
            "<container>\n" +
                "  <services>\n" +
                "      <service id=\"F<caret>oo\" class=\"DateTime\"/>\n" +
                "  </services>\n" +
                "</container>\n",
            "Symfony: lowercase letters for service and parameter"
        );

        assertLocalInspectionNotContains("service.xml",
                "<container>\n" +
                "  <services>\n" +
                "      <service id=\"f<caret>oo\" class=\"DateTime\"/>\n" +
                "  </services>\n" +
                "</container>\n",
            "Symfony: lowercase letters for service and parameter"
        );
    }

    public void testCaseSensitivityForYamlFiles() {
        assertLocalInspectionContains("service.yml", "services:\n" +
                "    foo<caret>_A:\n" +
                "        class: DateTime",
            "Symfony: lowercase letters for service and parameter"
        );

        assertLocalInspectionNotContains("service.yml", "services:\n" +
                "    foo<caret>_a:\n" +
                "        class: DateTime",
            "Symfony: lowercase letters for service and parameter"
        );

        assertLocalInspectionNotContains("service.yml", "services:\n" +
                "    foo<caret>_a:\n" +
                "        class: DateTime",
            "Symfony: lowercase letters for service and parameter"
        );

        assertLocalInspectionContains("service.yml", "parameters:\n" +
                "    F<caret>oo: bar",
            "Symfony: lowercase letters for service and parameter"
        );

        assertLocalInspectionNotContains("service.yml", "parameters:\n" +
                "    f<caret>oo: bar",
            "Symfony: lowercase letters for service and parameter"
        );
    }

    public void testCaseSensitivityForYamlExpressionsNotInspected() {
        assertLocalInspectionNotContains("service.yml","services:\n" +
                "    foo:\n" +
                "        arguments: [\"@=A<caret>aaaa\"]",
            "Symfony: lowercase letters for service and parameter"
        );
    }

    public void testCaseSensitivityForServiceInYamlFiles() {
        assertLocalInspectionContains("service.yml", "services:\n" +
                "    foo_a:\n" +
                "        arguments: [@f<caret>oO]",
            "Symfony: lowercase letters for service and parameter"
        );

        assertLocalInspectionContains("service.yml", "services:\n" +
                "    foo_a:\n" +
                "        arguments: ['@f<caret>oO']",
            "Symfony: lowercase letters for service and parameter"
        );

        assertLocalInspectionContains("service.yml", "services:\n" +
                "    foo_a:\n" +
                "        arguments: [\"@f<caret>oO\"]",
            "Symfony: lowercase letters for service and parameter"
        );

        assertLocalInspectionNotContains("service.yml", "services:\n" +
                "    foo_a:\n" +
                "        arguments: [@f<caret>o]",
            "Symfony: lowercase letters for service and parameter"
        );
    }
}
