package fr.adrienbrault.idea.symfony2plugin.tests.mcp

import fr.adrienbrault.idea.symfony2plugin.Settings
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.TwigComponentCollector
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.util.getSymfonyVarDirectoryWatcher

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigComponentCollector
 */
class TwigComponentCollectorTest : SymfonyLightCodeInsightFixtureTestCase() {
    private var previousContainerFiles: List<ContainerFile> = emptyList()

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        val settings = Settings.getInstance(project)
        previousContainerFiles = settings.containerFiles?.toList() ?: emptyList()
        settings.containerFiles = arrayListOf()

        myFixture.copyFileToProject("twig_component.yaml", "config/packages/twig_component.yaml")
        myFixture.copyFileToProject("ide-twig.json", "ide-twig.json")
    }

    @Throws(Exception::class)
    override fun tearDown() {
        try {
            Settings.getInstance(project).containerFiles = ArrayList(previousContainerFiles)
            getSymfonyVarDirectoryWatcher(project).reloadConfiguration()
        } finally {
            super.tearDown()
        }
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

    fun testCompiledContainerComponentIsIncluded() {
        configureContainerXml("""
            <service id="ux.twig_component.component_factory">
                <argument type="service" id="ux.twig_component.component_template_finder"/>
                <argument type="service" id=".service_locator.demo"/>
                <argument type="service" id="property_accessor"/>
                <argument type="service" id="event_dispatcher"/>
                <argument type="collection">
                    <argument key="Shop:Card" type="collection">
                        <argument key="class">App\Twig\Components\ShopCard</argument>
                        <argument key="template">components/shop/Card.html.twig</argument>
                    </argument>
                </argument>
                <argument type="collection">
                    <argument key="App\Twig\Components\ShopCard">Shop:Card</argument>
                </argument>
            </service>
        """.trimIndent())

        myFixture.addFileToProject("src/Twig/Components/ShopCard.php", "<?php\n" +
            "namespace App\\Twig\\Components;\n" +
            "\n" +
            "class ShopCard\n" +
            "{\n" +
            "    public string \$title = 'hello';\n" +
            "}\n"
        )
        myFixture.addFileToProject("templates/components/shop/Card.html.twig", "{% props variant %}{% block body %}{% endblock %}")

        val result = TwigComponentCollector(project).collect("shop")

        assertTrue(result.contains("<twig:Shop:Card></twig:Shop:Card>"))
        assertTrue(result.contains("templates/components/shop/Card.html.twig"))
        assertTrue(result.contains("title;variant"))
        assertTrue(result.contains("body"))
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

    private fun configureContainerXml(services: String) {
        val path = "var/cache/dev/${getTestName(false)}Container.xml"
        createFileInProjectRoot(path, """<?xml version="1.0" encoding="utf-8"?><container><services>$services</services></container>""")
        Settings.getInstance(project).containerFiles = arrayListOf(ContainerFile(path))
        getSymfonyVarDirectoryWatcher(project).reloadConfiguration()
    }
}
