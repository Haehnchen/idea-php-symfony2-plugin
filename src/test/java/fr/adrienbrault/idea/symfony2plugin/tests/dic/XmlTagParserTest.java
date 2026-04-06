package fr.adrienbrault.idea.symfony2plugin.tests.dic;

import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlTagParser;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.InputStream;
import java.util.Set;

public class XmlTagParserTest extends SymfonyLightCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic";
    }

    public void testParse() throws Exception {
        VirtualFile testFile = myFixture.copyFileToProject("appDevDebugProjectContainer.xml");

        XmlTagParser xmlTagParser = new XmlTagParser();
        try (InputStream inputStream = testFile.getInputStream()) {
            xmlTagParser.parser(inputStream, testFile, getProject());
        }
        Set<String> tags = xmlTagParser.get();

        assertTrue(tags.contains("twig.extension"));
        assertTrue(tags.contains("twig.extension.reloaded"));
        assertTrue(tags.contains("twig.extension.foo"));

        assertTrue(xmlTagParser.getTaggedClass("kernel.event_listener").contains("Symfony\\Bundle\\FrameworkBundle\\DataCollector\\RouterDataCollector"));
    }

}
