package fr.adrienbrault.idea.symfony2plugin.tests.dic.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.inspection.CaseSensitivityServiceInspection
 */
public class CaseSensitivityServiceInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testCaseSensitivityForXmlFiles() {
        assertLocalInspectionContains("service.xml",
            "<container>\n" +
                "  <services>\n" +
                "      <service id=\"F<caret>oo.Bar\" class=\"DateTime\"/>\n" +
                "  </services>\n" +
                "</container>\n",
            "Symfony: lowercase letters for service and parameter"
        );

        assertLocalInspectionNotContains("service.xml",
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

        assertLocalInspectionNotContains("service.yml", "parameters:\n" +
                "    f<caret>oo: bar",
            "Symfony: lowercase letters for service and parameter"
        );

        assertLocalInspectionNotContains("service.yml", "services:\n" +
                "    fo.o<caret>a:\n" +
                "        class: DateTime",
            "Symfony: lowercase letters for service and parameter"
        );

        assertLocalInspectionNotContains("service.yml", "parameters:\n" +
                "    F<caret>oo: bar",
            "Symfony: lowercase letters for service and parameter"
        );

        assertLocalInspectionNotContains("service.yml", "services:\n" +
                "    foo<caret>_A:\n" +
                "        class: DateTime",
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

    public void testCaseSensitivityForServicePhpFiles() {
        assertLocalInspectionContains("test.php", "<?php\n" +
                "/** @var $c \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "$c->get('@f<caret>oO');\n",
            "Symfony: lowercase letters for service and parameter"
        );

        assertLocalInspectionNotContains("test.php", "<?php\n" +
                "/** @var $c \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "$c->get('<caret>');\n",
            "Symfony: lowercase letters for service and parameter"
        );
    }

    public void testCaseSensitivityForServiceFilesMustNotHighlightClassesOfSymfony33() {
        assertLocalInspectionNotContains("service.yml", "services:\n" +
                "    My\\Sweet\\FooabrCl<caret>ass:\n" +
                "        class: DateTime",
            "Symfony: lowercase letters for service and parameter"
        );

        assertLocalInspectionNotContains("service.xml",
            "<container>\n" +
                "  <services>\n" +
                "      <service id=\"My\\Sweet\\Foo<caret>bar\"/>\n" +
                "  </services>\n" +
                "</container>\n",
            "Symfony: lowercase letters for service and parameter"
        );
    }
}
