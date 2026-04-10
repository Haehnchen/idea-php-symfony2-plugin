package fr.adrienbrault.idea.symfony2plugin.tests.dic;

import com.intellij.codeInsight.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerServiceMetadata;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceStringLookupElement;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.ServiceStringLookupElement
 */
public class ServiceStringLookupElementTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testServiceLookupPresentableRendering() {
        ContainerService service = new ContainerService("foo", "DateTime", new ContainerServiceMetadata(
            null,
            false,
            false,
            false,
            false,
            true,
            true,
            null,
            null,
            null,
            null,
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            ContainerServiceMetadata.SourceKind.INDEXED_SERVICE
        ));

        ServiceStringLookupElement element = new ServiceStringLookupElement(service);
        assertEquals("foo", element.getLookupString());

        LookupElementPresentation presentation = new LookupElementPresentation();
        element.renderElement(presentation);

        assertEquals("DateTime", presentation.getTypeText());
        assertTrue(presentation.isStrikeout());
    }

    public void testServiceLookupPresentableRenderingLegacy() {
        ServiceStringLookupElement element = new ServiceStringLookupElement(new ContainerService(
            "foo",
            "DateTime",
            ContainerServiceMetadata.empty(ContainerServiceMetadata.SourceKind.INDEXED_SERVICE)
        ));
        assertEquals("foo", element.getLookupString());

        LookupElementPresentation presentation = new LookupElementPresentation();
        element.renderElement(presentation);

        assertEquals("DateTime", presentation.getTypeText());
        assertFalse(presentation.isStrikeout());
    }
}
