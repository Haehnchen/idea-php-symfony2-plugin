package fr.adrienbrault.idea.symfony2plugin.tests.dic;

import fr.adrienbrault.idea.symfony2plugin.dic.ServiceMap;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceMapParser;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceInterface;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.ServiceMapParser
 */
public class ServiceMapParserTest extends Assert {

    @Test
    public void testParse() throws Exception {
        ServiceMapParser serviceMapParser = new ServiceMapParser();

        String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<container>" +
                "<service id=\"adrienbrault\" class=\"AdrienBrault\\Awesome\"/>" +
                "<service id=\"secret\" class=\"AdrienBrault\\Secret\" public=\"false\"/>" +
                "<service id=\"translator\" alias=\"adrienbrault\"/>" +
                "<service id=\"translator_private\" alias=\"adrienbrault\" public=\"false\"/>" +
            "</container>";

        ServiceMap serviceMap = serviceMapParser.parse(new ByteArrayInputStream(xmlString.getBytes()));

        assertTrue(serviceMap.getIds().contains("adrienbrault"));
        assertTrue(serviceMap.getIds().contains("secret"));
        assertTrue(serviceMap.getIds().contains("translator"));
        assertTrue(serviceMap.getIds().contains("translator_private"));

        ServiceInterface translator = serviceMap.getServices().stream().filter(s -> "translator".equals(s.getId())).findFirst().get();
        assertEquals("AdrienBrault\\Awesome", translator.getClassName());
        assertTrue("AdrienBrault\\Awesome", translator.isPublic());

        ServiceInterface translatorPrivate = serviceMap.getServices().stream().filter(s -> "translator_private".equals(s.getId())).findFirst().get();
        assertEquals("AdrienBrault\\Awesome", translatorPrivate.getClassName());
        assertFalse("AdrienBrault\\Awesome", translatorPrivate.isPublic());

        assertFalse(
            serviceMap.getServices().stream().filter(s -> "secret".equals(s.getId())).findFirst().get().isPublic()
        );
    }
}
