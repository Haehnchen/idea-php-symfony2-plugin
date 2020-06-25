package fr.adrienbrault.idea.symfony2plugin.tests.dic.container;

import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceInterface;
import fr.adrienbrault.idea.symfony2plugin.dic.container.XmlService;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.dic.container.XmlService
 */
public class XmlServiceTest extends Assert {
    @Test
    public void testThatServiceIsGenerated() {
        assertEquals(
            "foobar",
            createServiceFromTag("<service id=\"foobar\"/>").getId()
        );

        assertEquals(
            "alias",
            createServiceFromTag("<service id=\"foobar\" alias=\"alias\"/>").getAlias()
        );

        assertEquals(
            "MyClass",
            createServiceFromTag("<service id=\"foobar\" class=\"MyClass\"/>").getClassName()
        );

        assertFalse(
            createServiceFromTag("<service id=\"foobar\" public=\"false\"/>").isPublic()
        );

        assertNull(
            createServiceFromTag("<service class=\"foobar\"/>")
        );
    }

    @Test
    public void testThatTagsAreExtracted() {
        ServiceInterface serviceFromTag = createServiceFromTag("<service id=\"foobar\"><tag name=\"foo_tag\"/><argument type=\"service\" id=\"foo_argument\"/></service>");

        Collection<String> tags = serviceFromTag.getTags();
        assertEquals(1, tags.size());
        assertArrayEquals(new String[] {"foo_tag"}, tags.toArray(new String[0]));
    }

    private ServiceInterface createServiceFromTag(@NotNull String content) {
        return XmlService.createFromXml(createElementFromTag(content));
    }

    private Element createElementFromTag(@NotNull String content) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();

            Document document = builder.parse(new InputSource(new StringReader(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?><services>" + content + "</services>"
            )));

            return (Element) document.getElementsByTagName("service").item(0);
        } catch (Exception e) {
            return null;
        }
    }
}
