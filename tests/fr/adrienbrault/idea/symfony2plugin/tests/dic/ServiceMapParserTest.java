package fr.adrienbrault.idea.symfony2plugin.tests.dic;

import fr.adrienbrault.idea.symfony2plugin.dic.ServiceMap;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceMapParser;
import org.junit.Test;
import org.junit.Assert;

import java.io.ByteArrayInputStream;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
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
            "</container>";
        ServiceMap serviceMap = serviceMapParser.parse(new ByteArrayInputStream(xmlString.getBytes()));

        assertTrue(serviceMap instanceof ServiceMap);

        assertEquals("AdrienBrault\\Awesome", serviceMap.getMap().get("adrienbrault"));
        assertEquals("AdrienBrault\\Awesome", serviceMap.getPublicMap().get("adrienbrault"));

        assertEquals("AdrienBrault\\Secret", serviceMap.getMap().get("secret"));
        assertNull(serviceMap.getPublicMap().get("secret"));

        assertEquals("AdrienBrault\\Awesome", serviceMap.getMap().get("translator"));
        assertEquals("AdrienBrault\\Awesome", serviceMap.getPublicMap().get("translator"));
    }

}
