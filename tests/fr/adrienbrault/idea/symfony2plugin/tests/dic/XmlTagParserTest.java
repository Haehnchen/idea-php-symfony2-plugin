package fr.adrienbrault.idea.symfony2plugin.tests.dic;

import fr.adrienbrault.idea.symfony2plugin.dic.XmlTagParser;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;

public class XmlTagParserTest extends Assert {

    @Test
    public void testParse() throws Exception {

        File testFile = new File(this.getClass().getResource("appDevDebugProjectContainer.xml").getFile());

        XmlTagParser xmlTagParser = new XmlTagParser();
        xmlTagParser.parser(testFile);
        ArrayList<String> tags = xmlTagParser.get();

        assertTrue(tags.contains("twig.extension"));
        assertTrue(tags.contains("twig.extension.reloaded"));
        assertTrue(tags.contains("twig.extension.foo"));
    }

}
