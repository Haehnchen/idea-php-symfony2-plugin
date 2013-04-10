package fr.adrienbrault.idea.symfony2plugin.tests;

import fr.adrienbrault.idea.symfony2plugin.ServiceMap;
import fr.adrienbrault.idea.symfony2plugin.ServiceMapParser;
import org.junit.Test;
import org.junit.Assert;

import java.io.ByteArrayInputStream;
import java.util.Map;

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
            "</container>";
        ServiceMap serviceMap = serviceMapParser.parse(new ByteArrayInputStream(xmlString.getBytes()));

        assertTrue(serviceMap instanceof ServiceMap);

        assertEquals("\\AdrienBrault\\Awesome", serviceMap.getMap().get("adrienbrault"));
        assertEquals("\\AdrienBrault\\Awesome", serviceMap.getPublicMap().get("adrienbrault"));

        assertEquals("\\AdrienBrault\\Secret", serviceMap.getMap().get("secret"));
        assertNull(serviceMap.getPublicMap().get("secret"));

        assertEquals("\\Symfony\\Component\\HttpFoundation\\Request", serviceMap.getMap().get("request"));
        assertEquals("\\Symfony\\Component\\HttpFoundation\\Request", serviceMap.getPublicMap().get("request"));
        assertEquals("\\Symfony\\Component\\DependencyInjection\\ContainerInterface", serviceMap.getMap().get("service_container"));
        assertEquals("\\Symfony\\Component\\DependencyInjection\\ContainerInterface", serviceMap.getPublicMap().get("service_container"));
        assertEquals("\\Symfony\\Component\\HttpKernel\\KernelInterface", serviceMap.getMap().get("kernel"));
        assertEquals("\\Symfony\\Component\\HttpKernel\\KernelInterface", serviceMap.getPublicMap().get("kernel"));
    }

}
