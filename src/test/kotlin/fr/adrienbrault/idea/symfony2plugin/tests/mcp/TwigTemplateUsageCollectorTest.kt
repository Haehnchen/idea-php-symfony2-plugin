package fr.adrienbrault.idea.symfony2plugin.tests.mcp

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.TwigTemplateUsageCollector
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigTemplateUsageCollector
 */
class TwigTemplateUsageCollectorTest : SymfonyLightCodeInsightFixtureTestCase() {

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject("ide-twig.json",
            "{\"namespaces\":[{\"namespace\":\"\",\"path\":\"templates\"}]}"
        )
    }

    fun testCsvHeaderStructure() {
        val result = TwigTemplateUsageCollector(project).collect("nonexistent")

        assertTrue("Should have CSV header", result.startsWith(
            "template,controller,twig_include,twig_embed,twig_extends,twig_import,twig_use,twig_form_theme,twig_component,route_name,route_path"
        ))
    }

    fun testEmptyResultForNonMatchingTemplate() {
        val result = TwigTemplateUsageCollector(project).collect("nonexistent/template.html.twig")

        assertEquals(
            "template,controller,twig_include,twig_embed,twig_extends,twig_import,twig_use,twig_form_theme,twig_component,route_name,route_path\n",
            result
        )
    }

    fun testControllerUsageFullLine() {
        myFixture.addFileToProject("templates/base.html.twig", "")
        myFixture.addFileToProject("src/Controller/HomeController.php",
            "<?php\nnamespace App\\Controller;\n" +
            "class HomeController {\n" +
            "    public function index() { \$this->render('base.html.twig'); }\n" +
            "}\n"
        )

        val result = TwigTemplateUsageCollector(project).collect("base.html.twig")

        assertTrue("Unexpected CSV:\n$result", result.contains(
            "base.html.twig,App\\Controller\\HomeController::index,,,,,,,,"
        ))
    }

    fun testControllerRouteColumns() {
        myFixture.addFileToProject("templates/base.html.twig", "")
        myFixture.addFileToProject("src/Controller/HomeController.php",
            "<?php\nnamespace App\\Controller;\n" +
            "use Symfony\\Component\\Routing\\Attribute\\Route;\n" +
            "class HomeController {\n" +
            "    #[Route('/home', name: 'app_home')]\n" +
            "    public function index() { \$this->render('base.html.twig'); }\n" +
            "}\n"
        )

        val result = TwigTemplateUsageCollector(project).collect("base.html.twig")

        assertTrue("Route name expected:\n$result", result.contains("app_home"))
        assertTrue("Route path expected:\n$result", result.contains("/home"))
    }

    fun testTwigExtendsFullLine() {
        myFixture.addFileToProject("templates/base.html.twig", "")
        myFixture.addFileToProject("templates/child.html.twig", "{% extends 'base.html.twig' %}")

        val result = TwigTemplateUsageCollector(project).collect("base.html.twig")

        assertTrue("Unexpected CSV:\n$result", result.contains("base.html.twig,,,,"))
        assertTrue("Unexpected CSV:\n$result", result.contains("templates/child.html.twig,,,,,,\n"))
    }

    fun testTwigIncludeFullLine() {
        myFixture.addFileToProject("templates/base.html.twig", "")
        myFixture.addFileToProject("templates/page.html.twig", "{% include 'base.html.twig' %}")

        val result = TwigTemplateUsageCollector(project).collect("base.html.twig")

        assertTrue("Unexpected CSV:\n$result", result.contains("templates/page.html.twig,,,,,,,,\n"))
    }

    fun testTwigEmbedFullLine() {
        myFixture.addFileToProject("templates/base.html.twig", "")
        myFixture.addFileToProject("templates/embed_page.html.twig", "{% embed 'base.html.twig' %}{% endembed %}")

        val result = TwigTemplateUsageCollector(project).collect("base.html.twig")

        assertTrue("Unexpected CSV:\n$result", result.contains("templates/embed_page.html.twig,,,,,,,\n"))
    }

    fun testTwigImportFullLine() {
        myFixture.addFileToProject("templates/base.html.twig", "")
        myFixture.addFileToProject("templates/import_page.html.twig", "{% import 'base.html.twig' as macros %}")

        val result = TwigTemplateUsageCollector(project).collect("base.html.twig")

        assertTrue("Unexpected CSV:\n$result", result.contains("templates/import_page.html.twig,,,,,\n"))
    }

    fun testTwigFormThemeFullLine() {
        myFixture.addFileToProject("templates/base.html.twig", "")
        myFixture.addFileToProject("templates/form_theme.html.twig", "{% form_theme form with 'base.html.twig' %}")

        val result = TwigTemplateUsageCollector(project).collect("base.html.twig")

        assertTrue("Unexpected CSV:\n$result", result.contains("templates/form_theme.html.twig,,,\n"))
    }

    fun testAllUsageTypesInSingleRow() {
        myFixture.addFileToProject("templates/base.html.twig", "")
        myFixture.addFileToProject("src/Controller/HomeController.php",
            "<?php\nnamespace App\\Controller;\n" +
            "class HomeController {\n" +
            "    public function index() { \$this->render('base.html.twig'); }\n" +
            "}\n"
        )
        myFixture.addFileToProject("templates/child.html.twig",       "{% extends 'base.html.twig' %}")
        myFixture.addFileToProject("templates/page.html.twig",         "{% include 'base.html.twig' %}")
        myFixture.addFileToProject("templates/embed_page.html.twig",   "{% embed 'base.html.twig' %}{% endembed %}")
        myFixture.addFileToProject("templates/import_page.html.twig",  "{% import 'base.html.twig' as macros %}")
        myFixture.addFileToProject("templates/form_theme.html.twig",   "{% form_theme form with 'base.html.twig' %}")

        val result = TwigTemplateUsageCollector(project).collect("base.html.twig")

        assertTrue("Unexpected CSV:\n$result", result.contains(
            "base.html.twig,App\\Controller\\HomeController::index,"
        ))
        assertTrue("Unexpected CSV:\n$result", result.contains("templates/page.html.twig,"))
        assertTrue("Unexpected CSV:\n$result", result.contains("templates/embed_page.html.twig,"))
        assertTrue("Unexpected CSV:\n$result", result.contains("templates/child.html.twig,"))
        assertTrue("Unexpected CSV:\n$result", result.contains("templates/import_page.html.twig,"))
        assertTrue("Unexpected CSV:\n$result", result.contains("templates/form_theme.html.twig,"))
    }
}
