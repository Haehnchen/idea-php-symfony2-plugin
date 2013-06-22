package fr.adrienbrault.idea.symfony2plugin.tests.dic;

import fr.adrienbrault.idea.symfony2plugin.dic.XmlEventParser;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlTagParser;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;

public class XmlEventParserTest extends Assert {

    @Test
    public void testParse() throws Exception {

        File testFile = new File(this.getClass().getResource("appDevDebugProjectContainer.xml").getFile());

        XmlEventParser serviceMapParser = new XmlEventParser();
        serviceMapParser.parser(testFile);
        ArrayList<String> tags = serviceMapParser.get();

        assertTrue(tags.contains("kernel.controller"));
    }

}
