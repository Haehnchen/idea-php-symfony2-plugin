package fr.adrienbrault.idea.symfony2plugin.tests.mcp;

import fr.adrienbrault.idea.symfony2plugin.mcp.TwigComponentCollector;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigComponentCollector
 */
public class TwigComponentCollectorTest extends SymfonyLightCodeInsightFixtureTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("twig_component.yaml", "config/packages/twig_component.yaml");
        myFixture.copyFileToProject("ide-twig.json", "ide-twig.json");
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures";
    }

    public void testCsvHeaderStructure() {
        String result = new TwigComponentCollector(getProject()).collect("nonexistent");

        assertTrue(result.startsWith("component_name,template_relative_path,component_tag,twig_component_syntax,component_print_block_syntax,twig_tag_composition_syntax,props,template_blocks"));
    }

    public void testEmptyResultForNonMatchingComponent() {
        seedAlertFixtures();

        String result = new TwigComponentCollector(getProject()).collect("nonexistent_component");

        assertEquals("component_name,template_relative_path,component_tag,twig_component_syntax,component_print_block_syntax,twig_tag_composition_syntax,props,template_blocks\n", result);
    }

    public void testPartialSearchAndComponentSyntax() {
        seedAlertFixtures();

        myFixture.addFileToProject("src/Twig/Components/Button.php", "<?php\n" +
            "namespace App\\Twig\\Components;\n" +
            "\n" +
            "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "\n" +
            "#[AsTwigComponent('Button')]\n" +
            "class Button {}\n"
        );

        myFixture.addFileToProject("templates/components/Button.html.twig", "<button>Click</button>");

        String result = new TwigComponentCollector(getProject()).collect("ale");

        assertTrue(result.contains("<twig:Alert></twig:Alert>"));
        assertTrue(result.contains("{{ component('Alert') }}"));
        assertFalse(result.contains("<twig:Button></twig:Button>"));
    }

    public void testPropsBlocksAndRelativeTemplatePath() {
        seedAlertFixtures();

        String result = new TwigComponentCollector(getProject()).collect("Alert");
        String alertLine = null;
        for (String line : result.split("\n")) {
            if (line.startsWith("Alert,")) {
                alertLine = line;
                break;
            }
        }

        assertNotNull("Expected CSV row for Alert component.\nActual CSV:\n" + result, alertLine);

        String[] columns = alertLine.split(",", 8);
        assertEquals("Expected 8 CSV columns", 8, columns.length);

        assertEquals("Alert", columns[0]);
        assertTrue("Unexpected template path: " + columns[1], columns[1].endsWith("templates/components/Alert.html.twig"));
        assertEquals("<twig:Alert></twig:Alert>", columns[2]);
        assertEquals("{{ component('Alert') }}", columns[3]);
        assertEquals("{{ block('footer') }};{{ block('title') }}", columns[4]);
        assertEquals("{% component 'Alert' %}{% block footer %}{% endblock %}{% block title %}{% endblock %}{% endcomponent %}", columns[5]);
        assertEquals("icon;message;type", columns[6]);
        assertEquals("footer;title", columns[7]);
    }

    public void testAnonymousIndexTemplateComponentIsIncluded() {
        myFixture.addFileToProject("templates/components/Nav/index.html.twig", "{% props size %}{% block footer %}{% endblock %}");

        String result = new TwigComponentCollector(getProject()).collect("nav");

        assertTrue(result.contains("<twig:Nav></twig:Nav>"));
        assertTrue(result.contains("templates/components/Nav/index.html.twig"));
        assertTrue(result.contains("size"));
        assertTrue(result.contains("footer"));
    }

    private void seedAlertFixtures() {
        myFixture.addFileToProject("src/Twig/Components/Alert.php", "<?php\n" +
            "namespace App\\Twig\\Components;\n" +
            "\n" +
            "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "\n" +
            "#[AsTwigComponent('Alert')]\n" +
            "class Alert\n" +
            "{\n" +
            "    public string $message = 'hello';\n" +
            "}\n"
        );

        myFixture.addFileToProject("templates/components/Alert.html.twig", "{% props icon, type = 'info' %}\n" +
            "{% block footer %}{% endblock %}\n" +
            "{% block title %}{% endblock %}\n"
        );
    }
}
