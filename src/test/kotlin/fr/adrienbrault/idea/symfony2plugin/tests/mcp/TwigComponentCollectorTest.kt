package fr.adrienbrault.idea.symfony2plugin.tests.mcp

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.TwigComponentCollector
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigComponentCollector
 */
class TwigComponentCollectorTest : SymfonyLightCodeInsightFixtureTestCase() {

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("twig_component.yaml", "config/packages/twig_component.yaml")
        myFixture.copyFileToProject("ide-twig.json", "ide-twig.json")
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures"
    }

    fun testCsvHeaderStructure() {
        val result = TwigComponentCollector(project).collect("nonexistent")

        assertTrue(result.startsWith("component_name,template_relative_path,component_tag,twig_component_syntax,component_print_block_syntax,twig_tag_composition_syntax,props,template_blocks"))
    }

    fun testEmptyResultForNonMatchingComponent() {
        seedAlertFixtures()

        val result = TwigComponentCollector(project).collect("nonexistent_component")

        assertEquals("component_name,template_relative_path,component_tag,twig_component_syntax,component_print_block_syntax,twig_tag_composition_syntax,props,template_blocks\n", result)
    }

    fun testPartialSearchAndComponentSyntax() {
        seedAlertFixtures()

        myFixture.addFileToProject("src/Twig/Components/Button.php", "<?php\n" +
            "namespace App\\Twig\\Components;\n" +
            "\n" +
            "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "\n" +
            "#[AsTwigComponent('Button')]\n" +
            "class Button {}\n"
        )

        myFixture.addFileToProject("templates/components/Button.html.twig", "<button>Click</button>")

        val result = TwigComponentCollector(project).collect("ale")

        assertTrue(result.contains("<twig:Alert></twig:Alert>"))
        assertTrue(result.contains("{{ component('Alert') }}"))
        assertFalse(result.contains("<twig:Button></twig:Button>"))
    }

    fun testPropsBlocksAndRelativeTemplatePath() {
        seedAlertFixtures()

        val result = TwigComponentCollector(project).collect("Alert")
        var alertLine: String? = null
        for (line in result.split("\n")) {
            if (line.startsWith("Alert,")) {
                alertLine = line
                break
            }
        }

        assertNotNull("Expected CSV row for Alert component.\nActual CSV:\n$result", alertLine)

        val columns = alertLine!!.split(",", limit = 8).toTypedArray()
        assertEquals("Expected 8 CSV columns", 8, columns.size)

        assertEquals("Alert", columns[0])
        assertTrue("Unexpected template path: ${columns[1]}", columns[1].endsWith("templates/components/Alert.html.twig"))
        assertEquals("<twig:Alert></twig:Alert>", columns[2])
        assertEquals("{{ component('Alert') }}", columns[3])
        assertEquals("{{ block('footer') }};{{ block('title') }}", columns[4])
        assertEquals("{% component 'Alert' %}{% block footer %}{% endblock %}{% block title %}{% endblock %}{% endcomponent %}", columns[5])
        assertEquals("icon;message;type", columns[6])
        assertEquals("footer;title", columns[7])
    }

    fun testAnonymousIndexTemplateComponentIsIncluded() {
        myFixture.addFileToProject("templates/components/Nav/index.html.twig", "{% props size %}{% block footer %}{% endblock %}")

        val result = TwigComponentCollector(project).collect("nav")

        assertTrue(result.contains("<twig:Nav></twig:Nav>"))
        assertTrue(result.contains("templates/components/Nav/index.html.twig"))
        assertTrue(result.contains("size"))
        assertTrue(result.contains("footer"))
    }

    private fun seedAlertFixtures() {
        myFixture.addFileToProject("src/Twig/Components/Alert.php", "<?php\n" +
            "namespace App\\Twig\\Components;\n" +
            "\n" +
            "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "\n" +
            "#[AsTwigComponent('Alert')]\n" +
            "class Alert\n" +
            "{\n" +
            "    public string \$message = 'hello';\n" +
            "}\n"
        )

        myFixture.addFileToProject("templates/components/Alert.html.twig", "{% props icon, type = 'info' %}\n" +
            "{% block footer %}{% endblock %}\n" +
            "{% block title %}{% endblock %}\n"
        )
    }
}
