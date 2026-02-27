package fr.adrienbrault.idea.symfony2plugin.tests.mcp;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.mcp.TwigTemplateUsageCollector;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigTemplateUsageCollector
 */
public class TwigTemplateUsageCollectorTest extends SymfonyLightCodeInsightFixtureTestCase {

    /**
     * Verify CSV header structure
     */
    public void testCsvHeaderStructure() {
        String result = new TwigTemplateUsageCollector(getProject()).collect("nonexistent");

        assertTrue("Should have CSV header", result.startsWith("template,controller,twig_include,twig_embed,twig_extends,twig_import,twig_use,twig_form_theme,twig_component"));
    }

    /**
     * Empty result for non-matching template
     */
    public void testEmptyResultForNonMatchingTemplate() {
        String result = new TwigTemplateUsageCollector(getProject()).collect("nonexistent/template.html.twig");

        // Should only have header line
        assertEquals("template,controller,twig_include,twig_embed,twig_extends,twig_import,twig_use,twig_form_theme,twig_component\n", result);
    }
}
