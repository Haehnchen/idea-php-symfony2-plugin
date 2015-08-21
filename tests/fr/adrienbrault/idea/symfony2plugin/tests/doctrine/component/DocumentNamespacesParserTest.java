package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.component;

import fr.adrienbrault.idea.symfony2plugin.doctrine.component.DocumentNamespacesParser;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DocumentNamespacesParserTest extends Assert {

    @Test
    public void testParse() throws Exception {

        File testFile = new File(this.getClass().getResource("appDevDebugProjectContainer.xml").getFile());
        DocumentNamespacesParser entityNamesServiceParser = new DocumentNamespacesParser();
        entityNamesServiceParser.parser(testFile);
        Map<String, String> map = entityNamesServiceParser.getNamespaceMap();

        assertEquals("\\AcmeProject\\FrontendBundle\\Document", map.get("AcmeProjectFrontendBundle"));
        assertEquals("\\AcmeProject\\ApiBundle\\Document", map.get("AcmeProjectApiBundle"));
        assertEquals("\\AcmeProject\\CoreBundle\\Document", map.get("AcmeProjectCoreBundle"));
    }

}

