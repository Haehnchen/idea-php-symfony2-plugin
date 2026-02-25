package fr.adrienbrault.idea.symfony2plugin.tests.mcp;

import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.mcp.TwigTemplateUsageCollector;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigTemplateUsageCollector
 */
public class TwigTemplateUsageCollectorTest extends SymfonyLightCodeInsightFixtureTestCase {

    /**
     * PHP controller renders a template -> shows up in the controller column
     */
    public void testControllerUsage() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Controller;\n" +
            "class HomeController {\n" +
            "    public function index() {\n" +
            "        $this->render('home/index.html.twig');\n" +
            "    }\n" +
            "}"
        );

        String result = new TwigTemplateUsageCollector(getProject()).collect("home/index.html.twig");

        assertTrue(result.contains("home/index.html.twig"));
        assertTrue(result.contains("App\\Controller\\HomeController::index"));
    }

    /**
     * Twig file uses {% include %} -> caller shows up in the twig_include column
     */
    public void testTwigIncludeUsage() {
        myFixture.addFileToProject("templates/caller.html.twig",
            "{% include 'partials/nav.html.twig' %}"
        );

        String result = new TwigTemplateUsageCollector(getProject()).collect("partials/nav.html.twig");

        assertTrue(result.contains("partials/nav.html.twig"));
        assertTrue(result.contains("caller.html.twig"));
    }

    /**
     * Twig file uses {% extends %} -> caller shows up in the twig_extends column
     */
    public void testTwigExtendsUsage() {
        myFixture.addFileToProject("templates/child.html.twig",
            "{% extends 'layouts/base.html.twig' %}"
        );

        String result = new TwigTemplateUsageCollector(getProject()).collect("layouts/base.html.twig");

        assertTrue(result.contains("layouts/base.html.twig"));
        assertTrue(result.contains("child.html.twig"));
    }

    /**
     * Partial name filter only returns matching templates
     */
    public void testPartialFilter() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Controller;\n" +
            "class FooController {\n" +
            "    public function a() { $this->render('admin/users.html.twig'); }\n" +
            "    public function b() { $this->render('shop/products.html.twig'); }\n" +
            "}"
        );

        String result = new TwigTemplateUsageCollector(getProject()).collect("admin/");

        assertTrue(result.contains("admin/users.html.twig"));
        assertFalse(result.contains("shop/products.html.twig"));
    }
}
