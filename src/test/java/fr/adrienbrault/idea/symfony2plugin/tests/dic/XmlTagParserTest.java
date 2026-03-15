package fr.adrienbrault.idea.symfony2plugin.tests.dic;

import com.intellij.openapi.vfs.VfsUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlTagParser;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyTempCodeInsightFixtureTestCase;

import java.io.File;
import java.io.FileInputStream;
import java.util.Set;

public class XmlTagParserTest extends SymfonyTempCodeInsightFixtureTestCase {

    public void testParse() throws Exception {
        File testFile = new File("src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/appDevDebugProjectContainer.xml");

        XmlTagParser xmlTagParser = new XmlTagParser();
        xmlTagParser.parser(new FileInputStream(testFile), VfsUtil.findFileByIoFile(testFile, true), getProject());
        Set<String> tags = xmlTagParser.get();

        assertTrue(tags.contains("twig.extension"));
        assertTrue(tags.contains("twig.extension.reloaded"));
        assertTrue(tags.contains("twig.extension.foo"));

        assertTrue(xmlTagParser.getTaggedClass("kernel.event_listener").contains("Symfony\\Bundle\\FrameworkBundle\\DataCollector\\RouterDataCollector"));
    }

}
