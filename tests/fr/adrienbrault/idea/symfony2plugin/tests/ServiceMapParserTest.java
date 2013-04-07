package fr.adrienbrault.idea.symfony2plugin.tests;

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
                "<container><service id=\"adrienbrault\" class=\"AdrienBrault\\Awesome\"/></container>";
        Map<String, String> serviceMap = serviceMapParser.parse(new ByteArrayInputStream(xmlString.getBytes()));

        assertEquals("\\AdrienBrault\\Awesome", serviceMap.get("adrienbrault"));

        assertEquals("\\Symfony\\Component\\HttpFoundation\\Request", serviceMap.get("request"));
        assertEquals("\\Symfony\\Component\\DependencyInjection\\ContainerInterface", serviceMap.get("service_container"));
        assertEquals("\\Symfony\\Component\\HttpKernel\\KernelInterface", serviceMap.get("kernel"));
    }

}
