package fr.adrienbrault.idea.symfonyplugin.util.service;

import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
abstract public class AbstractServiceParser  implements ServiceParserInterface {

    @Nullable
    protected NodeList parserer(InputStream file) {

        Document document;
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder documentBuilder = dbFactory.newDocumentBuilder();
            document = documentBuilder.parse(file);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            return null;
        }

        if(document == null) {
            return null;
        }

        Object result;
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression xPathExpr = xpath.compile(this.getXPathFilter());
            result = xPathExpr.evaluate(document, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            return null;
        }

        return (NodeList) result;
    }

}
