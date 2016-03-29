package fr.adrienbrault.idea.symfony2plugin.config.yaml.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.util.Function;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.xerces.dom.CommentImpl;
import org.apache.xerces.dom.DeferredTextImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ConfigCompletionProvider extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

        PsiElement element = completionParameters.getPosition();
        if(!Symfony2ProjectComponent.isEnabled(element)) {
            return;
        }

        PsiElement yamlScalar = element.getParent();
        if(yamlScalar == null) {
            return;
        }

        PsiElement yamlCompount = yamlScalar.getParent();

        // yaml document root context
        if(yamlCompount.getParent() instanceof YAMLDocument) {
            attachRootConfig(completionResultSet, element);
            return;
        }

        // check inside yaml key value context
        if(!(yamlCompount instanceof YAMLCompoundValue || yamlCompount instanceof YAMLKeyValue)) {
            return;
        }

        // get all parent yaml keys
        List<String> items = YamlHelper.getParentArrayKeys(element);
        if(items.size() == 0) {
            return;
        }

        // normalize for xml
        items = ContainerUtil.map(items, new Function<String, String>() {
            @Override
            public String fun(String s) {
                return s.replace('_', '-');
            }
        });

        // reverse to get top most item first
        Collections.reverse(items);

        Document document = getConfigTemplate(element.getProject().getBaseDir());
        if(document == null) {
            return;
        }

        Node configNode = getMatchingConfigNode(document, items);
        if(configNode == null) {
            return;
        }

        getConfigPathLookupElements(completionResultSet, configNode, false);

        // map shortcuts like eg <dbal default-connection="">
        if(configNode instanceof Element) {
            NamedNodeMap attributes = configNode.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                String attributeName = attributes.item(i).getNodeName();
                if(attributeName.startsWith("default-")) {
                    Node defaultNode = getElementByTagNameWithUnPluralize((Element) configNode, attributeName.substring("default-".length()));
                    if(defaultNode != null) {
                        getConfigPathLookupElements(completionResultSet, defaultNode, true);
                    }
                }
            }
        }

    }

    private Document getConfigTemplate(VirtualFile projectBaseDir) {

        Document document;

        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            VirtualFile virtualFile = VfsUtil.findRelativeFile(projectBaseDir, ".idea", "symfony2-config.xml");
            if(virtualFile != null) {
                document = builder.parse(VfsUtil.virtualToIoFile(virtualFile));
            } else {
                document = builder.parse(ConfigCompletionProvider.class.getResourceAsStream("/resources/symfony2-config.xml"));
            }

            return document;

        } catch (ParserConfigurationException e) {
            return null;
        } catch (SAXException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    private void getConfigPathLookupElements(CompletionResultSet completionResultSet, Node configNode, boolean isShortcut) {

        // get config on node attributes
        NamedNodeMap attributes = configNode.getAttributes();
        if(attributes.getLength() > 0) {
            Map<String, String> nodeDocVars = getNodeCommentVars(configNode);
            for (int i = 0; i < attributes.getLength(); i++) {
                completionResultSet.addElement(getNodeAttributeLookupElement(attributes.item(i), nodeDocVars, isShortcut));
            }
        }


        // check for additional child node
        if(configNode instanceof Element) {

            NodeList nodeList1 = ((Element) configNode).getElementsByTagName("*");
            for (int i = 0; i < nodeList1.getLength(); i++) {
                LookupElementBuilder nodeTagLookupElement = getNodeTagLookupElement(nodeList1.item(i), isShortcut);
                if(nodeTagLookupElement != null) {
                    completionResultSet.addElement(nodeTagLookupElement);
                }
            }

        }


    }

    private LookupElementBuilder getNodeAttributeLookupElement(Node node, Map<String, String> nodeVars, boolean isShortcut) {

        String nodeName = getNodeName(node);
        LookupElementBuilder lookupElementBuilder = LookupElementBuilder.create(nodeName).withIcon(Symfony2Icons.CONFIG_VALUE);

        String textContent = node.getTextContent();
        if(StringUtils.isNotBlank(textContent)) {
            lookupElementBuilder = lookupElementBuilder.withTailText("(" + textContent + ")", true);
        }
        
        if(nodeVars.containsKey(nodeName)) {
            lookupElementBuilder = lookupElementBuilder.withTypeText(StringUtil.shortenTextWithEllipsis(nodeVars.get(nodeName), 100, 0), true);
        }

        if(isShortcut) {
            lookupElementBuilder = lookupElementBuilder.withIcon(Symfony2Icons.CONFIG_VALUE_SHORTCUT);
        }

        return lookupElementBuilder;
    }

    @Nullable
    private LookupElementBuilder getNodeTagLookupElement(Node node, boolean isShortcut) {

        String nodeName = getNodeName(node);
        boolean prototype = isPrototype(node);

        // prototype "connection" must be "connections" so pluralize
        if(prototype) {
            nodeName = StringUtil.pluralize(nodeName);
        }

        LookupElementBuilder lookupElementBuilder = LookupElementBuilder.create(nodeName).withIcon(Symfony2Icons.CONFIG_PROTOTYPE);

        if(prototype) {
            lookupElementBuilder = lookupElementBuilder.withTypeText("Prototype", true);
        }

        if(isShortcut) {
            lookupElementBuilder = lookupElementBuilder.withIcon(Symfony2Icons.CONFIG_VALUE_SHORTCUT);
        }

        return lookupElementBuilder;
    }

    private String getNodeName(Node node) {
        return node.getNodeName().replace("-", "_");
    }

    @Nullable
    private Element getElementByTagNameWithUnPluralize(@NotNull Element element, String tagName) {

        NodeList nodeList = element.getElementsByTagName(tagName);
        if(nodeList.getLength() > 0) {
            return (Element) nodeList.item(0);
        }

        String unpluralize = StringUtil.unpluralize(tagName);
        if(unpluralize == null) {
            return null;
        }

        nodeList = element.getElementsByTagName(unpluralize);
        if(nodeList.getLength() > 0) {
            return (Element) nodeList.item(0);
        }

        return null;

    }

    @Nullable
    private Element getElementByTagName(Document element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);

        if(nodeList.getLength() > 0) {
            return (Element) nodeList.item(0);
        }

        return null;

    }

    @Nullable
    private Node getMatchingConfigNode(Document document, List<String> items) {

        if(items.size() == 0) {
            return null;
        }

        Element currentNodeItem = getElementByTagName(document, items.get(0));
        if(currentNodeItem == null) {
            return null;
        }
        
        for (int i = 1; i < items.size(); i++) {

            currentNodeItem = getElementByTagNameWithUnPluralize(currentNodeItem, items.get(i));
            if(currentNodeItem == null) {
                return null;
            }

            if(isPrototype(currentNodeItem)) {
                i++;
            }

        }

        return currentNodeItem;

    }

    private boolean isPrototype(@Nullable Node node) {
        if(node == null) return false;

        Node previousSibling = node.getPreviousSibling();
        if(previousSibling == null) {
            return false;
        }

        // we can have multiple comments similar to docblock to a node
        // search for prototype
        Node comment = previousSibling.getPreviousSibling();
        while (comment instanceof CommentImpl || comment instanceof DeferredTextImpl) {

            if(comment instanceof CommentImpl) {
                if(comment.getTextContent().toLowerCase().matches("\\s*prototype.*")) {
                    return true;
                }
            }

            comment = comment.getPreviousSibling();
        }

        return false;
    }

    @NotNull
    private Map<String, String> getNodeCommentVars(@Nullable Node node) {
        Map<String, String> comments = new HashMap<String, String>();
        
        if(node == null) return comments;

        Node previousSibling = node.getPreviousSibling();
        if(previousSibling == comments) {
            return comments;
        }

        // get variable decl: "foo: test"
        Pattern compile = Pattern.compile("^\\s*([\\w_-]+)\\s*:\\s*(.*?)$");

        Node comment = previousSibling.getPreviousSibling();
        while (comment instanceof CommentImpl || comment instanceof DeferredTextImpl) {

            if(comment instanceof CommentImpl) {

                // try to find a var decl
                String trim = StringUtils.trim(comment.getTextContent());
                Matcher matcher = compile.matcher(trim);
                if (matcher.find()) {
                    comments.put(matcher.group(1).replace("-", "_"), matcher.group(2));
                }

            }

            comment = comment.getPreviousSibling();
        }

        return comments;
    }


    private void attachRootConfig(CompletionResultSet completionResultSet, PsiElement element) {

        Document document = getConfigTemplate(element.getProject().getBaseDir());
        if(document == null) {
            return;
        }

        try {

            // attach config aliases
            NodeList nodeList  = (NodeList) XPathFactory.newInstance().newXPath().compile("//config/*").evaluate(document, XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); i++) {
                completionResultSet.addElement(LookupElementBuilder.create(getNodeName(nodeList.item(i))).withIcon(Symfony2Icons.CONFIG_VALUE));
            }

        } catch (XPathExpressionException ignored) {
        }

    }

}
