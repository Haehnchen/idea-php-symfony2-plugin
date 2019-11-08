package fr.adrienbrault.idea.symfonyplugin.tests.dic;

import com.intellij.codeInsight.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfonyplugin.dic.ContainerService;
import fr.adrienbrault.idea.symfonyplugin.dic.ServiceStringLookupElement;
import fr.adrienbrault.idea.symfonyplugin.dic.container.SerializableService;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfonyplugin.dic.ServiceStringLookupElement
 */
public class ServiceStringLookupElementTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testServiceLookupPresentableRendering() {
        SerializableService service = new SerializableService("foo");
        service.setClassName("DateTime");
        service.setIsDeprecated(true);

        ServiceStringLookupElement element = new ServiceStringLookupElement(new ContainerService(service, null));
        assertEquals("foo", element.getLookupString());

        LookupElementPresentation presentation = new LookupElementPresentation();
        element.renderElement(presentation);

        assertEquals("DateTime", presentation.getTypeText());
        assertTrue(presentation.isStrikeout());
    }

    public void testServiceLookupPresentableRenderingLegacy() {
        ServiceStringLookupElement element = new ServiceStringLookupElement(new ContainerService("foo", "DateTime"));
        assertEquals("foo", element.getLookupString());

        LookupElementPresentation presentation = new LookupElementPresentation();
        element.renderElement(presentation);

        assertEquals("DateTime", presentation.getTypeText());
        assertFalse(presentation.isStrikeout());
    }
}
