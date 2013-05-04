package fr.adrienbrault.idea.symfony2plugin.doctrine.component;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EntityNamesParser {

    private DocumentBuilder documentBuilder;

    public EntityNamesParser() throws ParserConfigurationException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        documentBuilder = dbFactory.newDocumentBuilder();
    }

    public Map<String, String> parse(InputStream stream) throws IOException, SAXException {
        return parse(documentBuilder.parse(stream));
    }

    public Map<String, String> parse(File file) throws IOException, SAXException {
        return parse(documentBuilder.parse(file));
    }

    public Map<String, String> parse(Document document) {

        Map<String, String> map = new HashMap<String, String>();

        Object result = null;
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression xPathExpr = xpath.compile("/container/services/service[@id[starts-with(.,'doctrine.orm.')]]//call[@method='setEntityNamespaces']//argument[@key]");
            result = xPathExpr.evaluate(document, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            return map;
        }

        NodeList nodes = (NodeList) result;

        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            map.put(node.getAttribute("key"), "\\" + node.getTextContent());
        }

        return map;
    }

}
