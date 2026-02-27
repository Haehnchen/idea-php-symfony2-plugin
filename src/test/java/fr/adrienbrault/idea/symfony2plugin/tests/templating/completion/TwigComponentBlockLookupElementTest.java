package fr.adrienbrault.idea.symfony2plugin.tests.templating.completion;

import com.intellij.codeInsight.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfony2plugin.templating.completion.TwigComponentBlockLookupElement;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigComponentBlockLookupElementTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testLookupString() {
        TwigComponentBlockLookupElement element = new TwigComponentBlockLookupElement("message", "Alert.html.twig");
        assertEquals("twig:block name=\"message\"", element.getLookupString());
    }

    public void testLookupStringWithoutTypeText() {
        TwigComponentBlockLookupElement element = new TwigComponentBlockLookupElement("actions", null);
        assertEquals("twig:block name=\"actions\"", element.getLookupString());
    }

    public void testRenderElementWithTypeText() {
        TwigComponentBlockLookupElement element = new TwigComponentBlockLookupElement("message", "Alert.html.twig");
        LookupElementPresentation presentation = new LookupElementPresentation();
        element.renderElement(presentation);

        assertEquals("twig:block name=\"message\"", presentation.getItemText());
        assertEquals("Alert.html.twig", presentation.getTypeText());
        assertTrue(presentation.isTypeGrayed());
    }

    public void testRenderElementWithoutTypeText() {
        TwigComponentBlockLookupElement element = new TwigComponentBlockLookupElement("actions", null);
        LookupElementPresentation presentation = new LookupElementPresentation();
        element.renderElement(presentation);

        assertEquals("twig:block name=\"actions\"", presentation.getItemText());
        assertNull(presentation.getTypeText());
    }
}
