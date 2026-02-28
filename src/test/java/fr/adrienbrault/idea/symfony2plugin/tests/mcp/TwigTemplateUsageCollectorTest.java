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
            "template,controller,twig_include,twig_embed,twig_extends,twig_import,twig_use,twig_form_theme,twig_component"
        ));
    }

    public void testEmptyResultForNonMatchingTemplate() {
        String result = new TwigTemplateUsageCollector(getProject()).collect("nonexistent/template.html.twig");

        assertEquals(
            "template,controller,twig_include,twig_embed,twig_extends,twig_import,twig_use,twig_form_theme,twig_component\n",
            result
        );
    }

    /**
     * PHP controller rendering base.html.twig appears in the controller column (col 1).
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
     * {% extends 'base.html.twig' %} populates the twig_extends column (col 4).
     */
    public void testTwigExtendsFullLine() {
        myFixture.addFileToProject("templates/base.html.twig", "");
        myFixture.addFileToProject("templates/child.html.twig", "{% extends 'base.html.twig' %}");

        String result = new TwigTemplateUsageCollector(getProject()).collect("base.html.twig");

        // "base.html.twig,,,," = template + 3 empty (ctrl,incl,emb) + sep before extends
        assertTrue("Unexpected CSV:\n" + result, result.contains("base.html.twig,,,,"));
        // path ends with ",,,,\n" = extends path + 4 empty (import,use,form_theme,component)
        assertTrue("Unexpected CSV:\n" + result, result.contains("templates/child.html.twig,,,,\n"));
    }

    /**
     * {% include 'base.html.twig' %} populates the twig_include column (col 2).
     */
    public void testTwigIncludeFullLine() {
        myFixture.addFileToProject("templates/base.html.twig", "");
        myFixture.addFileToProject("templates/page.html.twig", "{% include 'base.html.twig' %}");

        String result = new TwigTemplateUsageCollector(getProject()).collect("base.html.twig");

        // path ends with ",,,,,,\n" = include path + 6 empty (emb,ext,imp,use,form,comp)
        assertTrue("Unexpected CSV:\n" + result, result.contains("templates/page.html.twig,,,,,,\n"));
    }

    /**
     * {% embed 'base.html.twig' %} populates the twig_embed column (col 3).
     */
    public void testTwigEmbedFullLine() {
        myFixture.addFileToProject("templates/base.html.twig", "");
        myFixture.addFileToProject("templates/embed_page.html.twig", "{% embed 'base.html.twig' %}{% endembed %}");

        String result = new TwigTemplateUsageCollector(getProject()).collect("base.html.twig");

        // path ends with ",,,,,\n" = embed path + 5 empty (ext,imp,use,form,comp)
        assertTrue("Unexpected CSV:\n" + result, result.contains("templates/embed_page.html.twig,,,,,\n"));
    }

    /**
     * {% import 'base.html.twig' as macros %} populates the twig_import column (col 5).
     */
    public void testTwigImportFullLine() {
        myFixture.addFileToProject("templates/base.html.twig", "");
        myFixture.addFileToProject("templates/import_page.html.twig", "{% import 'base.html.twig' as macros %}");

        String result = new TwigTemplateUsageCollector(getProject()).collect("base.html.twig");

        // path ends with ",,,\n" = import path + 3 empty (use,form_theme,component)
        assertTrue("Unexpected CSV:\n" + result, result.contains("templates/import_page.html.twig,,,\n"));
    }

    /**
     * {% form_theme form with 'base.html.twig' %} populates the twig_form_theme column (col 7).
     */
    public void testTwigFormThemeFullLine() {
        myFixture.addFileToProject("templates/base.html.twig", "");
        myFixture.addFileToProject("templates/form_theme.html.twig", "{% form_theme form with 'base.html.twig' %}");

        String result = new TwigTemplateUsageCollector(getProject()).collect("base.html.twig");

        // path ends with ",\n" = form_theme path + 1 empty (component)
        assertTrue("Unexpected CSV:\n" + result, result.contains("templates/form_theme.html.twig,\n"));
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

        // The combined row starts with "template,controller," — controller is present
        assertTrue("Unexpected CSV:\n" + result, result.contains(
            "base.html.twig,App\\Controller\\HomeController::index,"
        ));
        // Each relationship path appears in the single row; match the stable suffix+trailing-sep
        // (paths have an env-specific prefix such as "/src/", so we match the suffix only)
        assertTrue("Unexpected CSV:\n" + result, result.contains("templates/page.html.twig,"));
        assertTrue("Unexpected CSV:\n" + result, result.contains("templates/embed_page.html.twig,"));
        assertTrue("Unexpected CSV:\n" + result, result.contains("templates/child.html.twig,"));
        assertTrue("Unexpected CSV:\n" + result, result.contains("templates/import_page.html.twig,"));
        assertTrue("Unexpected CSV:\n" + result, result.contains("templates/form_theme.html.twig,"));
    }
}
