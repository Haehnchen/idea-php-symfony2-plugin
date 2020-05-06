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
                "<service id=\".secret.test\" class=\"AdrienBrault\\Secret\" public=\"false\"/>" +
                "<service id=\"translator\" alias=\"adrienbrault\"/>" +
                "<service id=\"translator_private\" alias=\"adrienbrault\" public=\"false\"/>" +
                "<service id=\".service_locator.SFX6J7Y\" class=\"Symfony\\Component\\DependencyInjection\\ServiceLocator\" public=\"false\"/>" +
                "<service id=\".instanceof.SFX6J7Y\" class=\"Symfony\\Component\\DependencyInjection\\ServiceLocator\" public=\"false\"/>" +
                "<service id=\".abstract.SFX6J7Y\" class=\"Symfony\\Component\\DependencyInjection\\ServiceLocator\" public=\"false\"/>" +
                "<service id=\".debug.SFX6J7Y\" class=\"Symfony\\Component\\DependencyInjection\\ServiceLocator\" public=\"false\"/>" +
                "<service id=\".errored.SFX6J7Y\" class=\"Symfony\\Component\\DependencyInjection\\ServiceLocator\" public=\"false\"/>" +
                "<service id=\"Psr\\Log\\LoggerInterface $securityLogger\" alias=\"monolog.logger.security\"/>" +
                "<service id=\".1_~NpzP6Xn\" public=\"false\"/>" +
                "<service id=\".2_PhpArrayAdapter~kSL.YwK\" class=\"Symfony\\Component\\Cache\\Adapter\\PhpArrayAdapter\" public=\"false\"/>" +
                "<service id=\".1_RouteLoaderContainer~Na7uo_Q\" class=\"Symfony\\Component\\Cache\\Adapter\\PhpArrayAdapter\" public=\"false\"/>" +
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

        assertEquals(1, serviceMap.getServices().stream().filter(s -> ".secret.test".equals(s.getId())).count());

        assertEquals(0, serviceMap.getServices().stream().filter(s -> ".service_locator.SFX6J7Y".equals(s.getId())).count());
        assertEquals(0, serviceMap.getServices().stream().filter(s -> ".service_locator.SFX6J7Y".equals(s.getId())).count());
        assertEquals(0, serviceMap.getServices().stream().filter(s -> ".1_~NpzP6Xn".equals(s.getId())).count());
        assertEquals(0, serviceMap.getServices().stream().filter(s -> ".2_PhpArrayAdapter~kSL.YwK".equals(s.getId())).count());
        assertEquals(0, serviceMap.getServices().stream().filter(s -> ".1_RouteLoaderContainer~Na7uo_Q".equals(s.getId())).count());
        assertEquals(0, serviceMap.getServices().stream().filter(s -> ".abstract.SFX6J7Y".equals(s.getId())).count());
        assertEquals(0, serviceMap.getServices().stream().filter(s -> ".instanceof.SFX6J7Y".equals(s.getId())).count());
        assertEquals(0, serviceMap.getServices().stream().filter(s -> ".debug.SFX6J7Y".equals(s.getId())).count());
        assertEquals(0, serviceMap.getServices().stream().filter(s -> ".errored.SFX6J7Y".equals(s.getId())).count());
    }
}
