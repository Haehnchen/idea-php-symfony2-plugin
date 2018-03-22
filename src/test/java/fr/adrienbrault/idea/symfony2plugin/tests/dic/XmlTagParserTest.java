package fr.adrienbrault.idea.symfony2plugin.tests.dic;

import fr.adrienbrault.idea.symfony2plugin.dic.XmlTagParser;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Set;

public class XmlTagParserTest extends Assert {

    @Test
    public void testParse() throws Exception {

        File testFile = new File("src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/appDevDebugProjectContainer.xml");

        XmlTagParser xmlTagParser = new XmlTagParser();
        xmlTagParser.parser(new FileInputStream(testFile));
        Set<String> tags = xmlTagParser.get();

        assertTrue(tags.contains("twig.extension"));
        assertTrue(tags.contains("twig.extension.reloaded"));
        assertTrue(tags.contains("twig.extension.foo"));

        assertTrue(xmlTagParser.getTaggedClass("kernel.event_listener").contains("Symfony\\Bundle\\FrameworkBundle\\DataCollector\\RouterDataCollector"));
    }

}
