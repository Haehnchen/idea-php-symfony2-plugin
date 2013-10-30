package fr.adrienbrault.idea.symfony2plugin.tests.translation.parser;

import fr.adrienbrault.idea.symfony2plugin.translation.dict.DomainFileMap;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.DomainMappings;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class DomainMappingsTest extends Assert {

    @Test
    public void testParser() {
        File testFile = new File(this.getClass().getResource("appDevDebugProjectContainer.xml").getFile());

        DomainMappings bla = new DomainMappings();
        bla.parser(testFile);
        List<DomainFileMap> map = bla.getDomainFileMaps();

        assertEquals(10, map.size());

        DomainFileMap firstDomain = map.iterator().next();

        assertEquals("CraueFormFlowBundle", firstDomain.getDomain());
        assertEquals("de", firstDomain.getLanguageKey());
        assertEquals("yml", firstDomain.getLoader());
        assertTrue(firstDomain.getPath().endsWith("CraueFormFlowBundle.de.yml"));

    }

}
