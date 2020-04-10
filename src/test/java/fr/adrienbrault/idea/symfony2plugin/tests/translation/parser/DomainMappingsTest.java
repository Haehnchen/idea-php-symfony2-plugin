package fr.adrienbrault.idea.symfony2plugin.tests.translation.parser;

import fr.adrienbrault.idea.symfony2plugin.translation.dict.DomainFileMap;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.DomainMappings;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DomainMappingsTest extends Assert {
    @Test
    public void testParser() throws FileNotFoundException {
        File testFile = new File("src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/translation/parser/appDevDebugProjectContainer.xml");

        DomainMappings domainMappings = new DomainMappings();
        domainMappings.parser(new FileInputStream(testFile));
        Collection<DomainFileMap> domainFileMaps = domainMappings.getDomainFileMaps();

        assertEquals(14, domainFileMaps.size());

        DomainFileMap craueFormFlowBundle = domainFileMaps.stream().filter(domainFileMap -> domainFileMap.getDomain().equals("CraueFormFlowBundle")).findFirst().get();

        assertEquals("CraueFormFlowBundle", craueFormFlowBundle.getDomain());
        assertEquals("de", craueFormFlowBundle.getLanguageKey());
        assertEquals("yml", craueFormFlowBundle.getLoader());
        assertTrue(craueFormFlowBundle.getPath().endsWith("CraueFormFlowBundle.de.yml"));

        DomainFileMap foobarDomain1 = domainFileMaps.stream().filter(domainFileMap -> domainFileMap.getDomain().equals("foobar_domain1")).findFirst().get();
        assertEquals("foobar_domain1", foobarDomain1.getDomain());
        assertEquals("af", foobarDomain1.getLanguageKey());
        assertEquals("xlf", foobarDomain1.getLoader());
        assertTrue(foobarDomain1.getPath().endsWith("foobar_domain1.af.xlf"));

        assertNotNull(domainFileMaps.stream().filter(domainFileMap -> domainFileMap.getDomain().equals("foobar_domain2")).findFirst().get());
        assertNotNull(domainFileMaps.stream().filter(domainFileMap -> domainFileMap.getDomain().equals("foobar_domain3")).findFirst().get());
        assertNotNull(domainFileMaps.stream().filter(domainFileMap -> domainFileMap.getDomain().equals("foobar_domain4")).findFirst().get());
    }
}
