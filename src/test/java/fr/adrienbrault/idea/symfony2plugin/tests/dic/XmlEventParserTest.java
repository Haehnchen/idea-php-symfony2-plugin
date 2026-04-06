package fr.adrienbrault.idea.symfony2plugin.tests.dic;

import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlEventParser;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.InputStream;
import java.util.Map;

public class XmlEventParserTest extends SymfonyLightCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic";
    }

    public void testParse() throws Exception {
        VirtualFile testFile = myFixture.copyFileToProject("appDevDebugProjectContainer.xml");

        XmlEventParser serviceMapParser = new XmlEventParser();
        try (InputStream inputStream = testFile.getInputStream()) {
            serviceMapParser.parser(inputStream, testFile, getProject());
        }
        Map<String, String> tags = serviceMapParser.get();

        assertTrue(tags.containsKey("kernel.controller"));
        assertEquals("kernel.event_listener", tags.get("kernel.controller"));

        assertTrue(!serviceMapParser.getEventSubscribers("kernel.controller").isEmpty());
        assertEquals("Symfony\\Bundle\\FrameworkBundle\\DataCollector\\RouterDataCollector", serviceMapParser.getEventSubscribers("kernel.controller").get(0).getFqnClassName());
    }

}
