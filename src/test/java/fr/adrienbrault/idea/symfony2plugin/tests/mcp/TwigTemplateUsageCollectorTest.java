package fr.adrienbrault.idea.symfony2plugin.tests.mcp;

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.TwigTemplateUsageCollector;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigTemplateUsageCollector
 */
public class TwigTemplateUsageCollectorTest extends SymfonyLightCodeInsightFixtureTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        myFixture.addFileToProject("ide-twig.json",
            "{\"namespaces\":[{\"namespace\":\"\",\"path\":\"templates\"}]}"
        );
    }

    public void testCsvHeaderStructure() {
        String result = new TwigTemplateUsageCollector(getProject()).collect("nonexistent");

        assertTrue("Should have CSV header", result.startsWith(
            "template,controller,twig_include,twig_embed,twig_extends,twig_import,twig_use,twig_form_theme,twig_component,route_name,route_path"
        ));
    }

    public void testEmptyResultForNonMatchingTemplate() {
        String result = new TwigTemplateUsageCollector(getProject()).collect("nonexistent/template.html.twig");

        assertEquals(
            "template,controller,twig_include,twig_embed,twig_extends,twig_import,twig_use,twig_form_theme,twig_component,route_name,route_path\n",
            result
        );
    }

    /**
     * PHP controller rendering base.html.twig appears in the controller column.
     */
    public void testControllerUsageFullLine() {
        myFixture.addFileToProject("templates/base.html.twig", "");
        myFixture.addFileToProject("src/Controller/HomeController.php",
            "<?php\nnamespace App\\Controller;\n" +
            "class HomeController {\n" +
            "    public function index() { $this->render('base.html.twig'); }\n" +
            "}\n"
        );

        String result = new TwigTemplateUsageCollector(getProject()).collect("base.html.twig");

        assertTrue("Unexpected CSV:\n" + result, result.contains(
            "base.html.twig,App\\Controller\\HomeController::index,,,,,,,"
        ));
    }

    /**
     * Controller with a #[Route] attribute populates the route_name and route_path columns.
     */
    public void testControllerRouteColumns() {
        myFixture.addFileToProject("templates/base.html.twig", "");
        myFixture.addFileToProject("src/Controller/HomeController.php",
            "<?php\nnamespace App\\Controller;\n" +
            "use Symfony\\Component\\Routing\\Attribute\\Route;\n" +
            "class HomeController {\n" +
            "    #[Route('/home', name: 'app_home')]\n" +
            "    public function index() { $this->render('base.html.twig'); }\n" +
            "}\n"
        );

        String result = new TwigTemplateUsageCollector(getProject()).collect("base.html.twig");

        assertTrue("Route name expected:\n" + result, result.contains("app_home"));
        assertTrue("Route path expected:\n" + result, result.contains("/home"));
    }

    /**
     * {% extends 'base.html.twig' %} populates the twig_extends column.
     */
    public void testTwigExtendsFullLine() {
        myFixture.addFileToProject("templates/base.html.twig", "");
        myFixture.addFileToProject("templates/child.html.twig", "{% extends 'base.html.twig' %}");

        String result = new TwigTemplateUsageCollector(getProject()).collect("base.html.twig");

        assertTrue("Unexpected CSV:\n" + result, result.contains("base.html.twig,,,,"));
        assertTrue("Unexpected CSV:\n" + result, result.contains("templates/child.html.twig,,,,,,\n"));
    }

    /**
     * {% include 'base.html.twig' %} populates the twig_include column.
     */
    public void testTwigIncludeFullLine() {
        myFixture.addFileToProject("templates/base.html.twig", "");
        myFixture.addFileToProject("templates/page.html.twig", "{% include 'base.html.twig' %}");

        String result = new TwigTemplateUsageCollector(getProject()).collect("base.html.twig");

        assertTrue("Unexpected CSV:\n" + result, result.contains("templates/page.html.twig,,,,,,,,\n"));
    }

    /**
     * {% embed 'base.html.twig' %} populates the twig_embed column.
     */
    public void testTwigEmbedFullLine() {
        myFixture.addFileToProject("templates/base.html.twig", "");
        myFixture.addFileToProject("templates/embed_page.html.twig", "{% embed 'base.html.twig' %}{% endembed %}");

        String result = new TwigTemplateUsageCollector(getProject()).collect("base.html.twig");

        assertTrue("Unexpected CSV:\n" + result, result.contains("templates/embed_page.html.twig,,,,,,,\n"));
    }

    /**
     * {% import 'base.html.twig' as macros %} populates the twig_import column.
     */
    public void testTwigImportFullLine() {
        myFixture.addFileToProject("templates/base.html.twig", "");
        myFixture.addFileToProject("templates/import_page.html.twig", "{% import 'base.html.twig' as macros %}");

        String result = new TwigTemplateUsageCollector(getProject()).collect("base.html.twig");

        assertTrue("Unexpected CSV:\n" + result, result.contains("templates/import_page.html.twig,,,,,\n"));
    }

    /**
     * {% form_theme form with 'base.html.twig' %} populates the twig_form_theme column.
     */
    public void testTwigFormThemeFullLine() {
        myFixture.addFileToProject("templates/base.html.twig", "");
        myFixture.addFileToProject("templates/form_theme.html.twig", "{% form_theme form with 'base.html.twig' %}");

        String result = new TwigTemplateUsageCollector(getProject()).collect("base.html.twig");

        assertTrue("Unexpected CSV:\n" + result, result.contains("templates/form_theme.html.twig,,,\n"));
    }

    /**
     * All supported usage types simultaneously referencing base.html.twig produce a single
     * row where every column is populated with the expected file.
     */
    public void testAllUsageTypesInSingleRow() {
        myFixture.addFileToProject("templates/base.html.twig", "");
        myFixture.addFileToProject("src/Controller/HomeController.php",
            "<?php\nnamespace App\\Controller;\n" +
            "class HomeController {\n" +
            "    public function index() { $this->render('base.html.twig'); }\n" +
            "}\n"
        );
        myFixture.addFileToProject("templates/child.html.twig",       "{% extends 'base.html.twig' %}");
        myFixture.addFileToProject("templates/page.html.twig",         "{% include 'base.html.twig' %}");
        myFixture.addFileToProject("templates/embed_page.html.twig",   "{% embed 'base.html.twig' %}{% endembed %}");
        myFixture.addFileToProject("templates/import_page.html.twig",  "{% import 'base.html.twig' as macros %}");
        myFixture.addFileToProject("templates/form_theme.html.twig",   "{% form_theme form with 'base.html.twig' %}");

        String result = new TwigTemplateUsageCollector(getProject()).collect("base.html.twig");

        assertTrue("Unexpected CSV:\n" + result, result.contains(
            "base.html.twig,App\\Controller\\HomeController::index,"
        ));
        assertTrue("Unexpected CSV:\n" + result, result.contains("templates/page.html.twig,"));
        assertTrue("Unexpected CSV:\n" + result, result.contains("templates/embed_page.html.twig,"));
        assertTrue("Unexpected CSV:\n" + result, result.contains("templates/child.html.twig,"));
        assertTrue("Unexpected CSV:\n" + result, result.contains("templates/import_page.html.twig,"));
        assertTrue("Unexpected CSV:\n" + result, result.contains("templates/form_theme.html.twig,"));
    }
}
