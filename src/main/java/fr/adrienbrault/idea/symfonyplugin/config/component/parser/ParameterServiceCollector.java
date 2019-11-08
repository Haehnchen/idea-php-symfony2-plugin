package fr.adrienbrault.idea.symfonyplugin.config.component.parser;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ParameterServiceCollector {

    @NotNull
    public static Map<String, String> collect(InputStream stream) {
        try {
            return collect(DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(stream)
            );
        } catch (IOException | SAXException | ParserConfigurationException e) {
            return Collections.emptyMap();
        }
    }

    @NotNull
    public static Map<String, String> collect(File file) {
        try {
            return collect(DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(file)
            );
        } catch (SAXException | IOException | ParserConfigurationException e) {
            return Collections.emptyMap();
        }
    }

    @NotNull
    private static Map<String, String> collect(Document document) {

        Map<String, String> parameterMap = new ConcurrentHashMap<>();

        Object nodeList;
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression xPathExpr = xpath.compile("/container/parameters/parameter[@key]");
            nodeList = xPathExpr.evaluate(document, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            return Collections.emptyMap();
        }

        if(!(nodeList instanceof NodeList)) {
            return Collections.emptyMap();
        }

        for (int i = 0; i < ((NodeList) nodeList).getLength(); i++) {
            Element node = (Element) ((NodeList) nodeList).item(i);
            String parameterValue = node.hasAttribute("type") && node.getAttribute("type").equals("collection") ?  "collection" : node.getTextContent();
            parameterMap.put(node.getAttribute("key"), parameterValue);
        }

        return parameterMap;
    }

}
