package fr.adrienbrault.idea.symfony2plugin.tests.translation.parser;

import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.DomainFileMap;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.DomainMappings;

import java.io.InputStream;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DomainMappingsTest extends SymfonyLightCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/translation/parser";
    }

    public void testParser() throws Exception {
        VirtualFile testFile = myFixture.copyFileToProject("appDevDebugProjectContainer.xml");

        DomainMappings domainMappings = new DomainMappings();
        try (InputStream inputStream = testFile.getInputStream()) {
            domainMappings.parser(inputStream, testFile, getProject());
        }
        Collection<DomainFileMap> domainFileMaps = domainMappings.getDomainFileMaps();

        assertEquals(16, domainFileMaps.size());

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

        DomainFileMap foobarIntl = domainFileMaps.stream().filter(domainFileMap -> domainFileMap.getDomain().equals("foobar_intl")).findFirst().get();
        assertEquals("foobar_intl", foobarIntl.getDomain());
        assertEquals("de", foobarIntl.getLanguageKey());
        assertEquals("xlf", foobarIntl.getLoader());
        assertTrue(foobarIntl.getPath().endsWith("foobar_intl+intl-icu.de.xlf"));

        DomainFileMap foobarOldIntl = domainFileMaps.stream().filter(domainFileMap -> domainFileMap.getDomain().equals("foobar_old_intl")).findFirst().get();
        assertEquals("foobar_old_intl", foobarOldIntl.getDomain());
        assertEquals("de", foobarOldIntl.getLanguageKey());
        assertEquals("xlf", foobarOldIntl.getLoader());
        assertTrue(foobarOldIntl.getPath().endsWith("foobar_old_intl+intl-icu.de.xlf"));
    }
}
