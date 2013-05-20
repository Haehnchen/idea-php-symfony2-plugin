package fr.adrienbrault.idea.symfony2plugin.form.dict;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormTypeParser {
    private DocumentBuilder documentBuilder;

    public FormTypeParser() throws ParserConfigurationException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        documentBuilder = dbFactory.newDocumentBuilder();
    }

    public FormTypeMap parse(InputStream stream) throws IOException, SAXException {
        return parse(documentBuilder.parse(stream));
    }

    public FormTypeMap parse(File file) throws IOException, SAXException {
        return parse(documentBuilder.parse(file));
    }

    public FormTypeMap parse(Document document) {

        Map<String, String> fromTypesMap = new HashMap<String, String>();

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();

        XPathExpression xPathExpr = null;

        try {
            xPathExpr = xpath.compile("/container/services/service[@id='form.registry']//service[@class]/argument[@type='collection'][1]/argument[@key]");
        } catch (XPathExpressionException e) {
            return new FormTypeMap(fromTypesMap);
        }

        Object result = null;
        try {
            result = xPathExpr.evaluate(document, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            return new FormTypeMap(fromTypesMap);
        }

        NodeList nodes = (NodeList) result;

        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            fromTypesMap.put(node.getTextContent(), node.getAttribute("key"));
        }

        return new FormTypeMap(fromTypesMap);
    }

}
