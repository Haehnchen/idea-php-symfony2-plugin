package fr.adrienbrault.idea.symfony2plugin.tests.dic;

import fr.adrienbrault.idea.symfony2plugin.dic.XmlEventParser;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

public class XmlEventParserTest extends Assert {

    @Test
    public void testParse() throws Exception {

        File testFile = new File(this.getClass().getResource("appDevDebugProjectContainer.xml").getFile());

        XmlEventParser serviceMapParser = new XmlEventParser();
        serviceMapParser.parser(new FileInputStream(testFile));
        Map<String, String> tags = serviceMapParser.get();

        assertTrue(tags.containsKey("kernel.controller"));
        assertEquals("kernel.event_listener", tags.get("kernel.controller"));

        assertTrue(serviceMapParser.getEventSubscribers("kernel.controller").size() > 0);
        assertEquals("Symfony\\Bundle\\FrameworkBundle\\DataCollector\\RouterDataCollector", serviceMapParser.getEventSubscribers("kernel.controller").get(0).getFqnClassName());
    }

}
