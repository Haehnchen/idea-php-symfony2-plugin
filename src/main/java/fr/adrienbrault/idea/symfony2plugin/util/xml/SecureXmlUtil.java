package fr.adrienbrault.idea.symfony2plugin.util.xml;

import org.jetbrains.annotations.NotNull;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.StringReader;

/**
 * Creates DOM parsers hardened against XXE-style file and network access.
 */
public final class SecureXmlUtil {
    private SecureXmlUtil() {
    }

    @NotNull
    private static DocumentBuilderFactory createDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        clearAttribute(factory, XMLConstants.ACCESS_EXTERNAL_DTD);
        clearAttribute(factory, XMLConstants.ACCESS_EXTERNAL_SCHEMA);

        return factory;
    }

    /**
     * Returns a new {@link DocumentBuilder} with DTDs and external entities disabled.
     */
    @NotNull
    public static DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilder builder = createDocumentBuilderFactory().newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
        return builder;
    }

    private static void clearAttribute(@NotNull DocumentBuilderFactory factory, @NotNull String name) {
        try {
            factory.setAttribute(name, "");
        } catch (IllegalArgumentException e) {
            // Some XML parser implementations do not support the JAXP external access attributes.
        }
    }
}
