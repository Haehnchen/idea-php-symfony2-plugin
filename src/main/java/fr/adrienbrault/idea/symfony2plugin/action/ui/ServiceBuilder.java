package fr.adrienbrault.idea.symfony2plugin.action.ui;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceTag;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceBuilder {

    public enum OutputType {
        Yaml, XML,
    }

    private final List<MethodParameter.MethodModelParameter>  methodModelParameter;
    private final Project project;

    /**
     * Symfony 3.3 class name can be id attribute for services
     */
    private final boolean isClassAsIdAttribute;

    @Nullable
    private PsiFile psiFile;

    public ServiceBuilder(@NotNull List<MethodParameter.MethodModelParameter> methodModelParameter, @NotNull Project project, boolean isClassAsIdAttribute) {
        this.methodModelParameter = methodModelParameter;
        this.project = project;
        this.isClassAsIdAttribute = isClassAsIdAttribute;
    }

    public ServiceBuilder(@NotNull List<MethodParameter.MethodModelParameter> methodModelParameter, @NotNull PsiFile psiFile, boolean isClassAsIdAttribute) {
        this.methodModelParameter = methodModelParameter;
        this.project = psiFile.getProject();
        this.psiFile = psiFile;
        this.isClassAsIdAttribute = isClassAsIdAttribute;
    }

    @Nullable
    public String build(OutputType outputType, String className, String serviceName) {
        HashMap<String, List<MethodParameter.MethodModelParameter>> methods = new HashMap<>();

        for(MethodParameter.MethodModelParameter methodModelParameter: this.methodModelParameter) {

            String methodName = methodModelParameter.getName();
            if(methodModelParameter.getMethod().getMethodType(false) == Method.MethodType.CONSTRUCTOR) {
                methodName = "__construct";
            }

            if(methods.containsKey(methodModelParameter.getName())) {
                methods.get(methodName).add(methodModelParameter);
            } else {
                methods.put(methodName, new ArrayList<>(Collections.singletonList(methodModelParameter)));
            }
        }

        if(outputType == OutputType.Yaml) {
            return buildYaml(methods, className, serviceName);
        }

        if(outputType == OutputType.XML) {
            return buildXml(methods, className, serviceName);
        }

        return null;
    }

    @Nullable
    private List<String> getParameters(List<MethodParameter.MethodModelParameter> methodModelParameters) {
        boolean hasCall = false;
        List<String> methodCalls = new ArrayList<>();

        // sort by indexes parameter
        methodModelParameters.sort(Comparator.comparingInt(MethodParameter.MethodModelParameter::getIndex));

        for(MethodParameter.MethodModelParameter methodModelParameter: methodModelParameters) {

            // only add items which have at least one service parameter
            if(!hasCall && methodModelParameter.isPossibleService()) {
                hasCall = true;
            }

            // missing required parameter; add to service template, so use can correct it after
            String currentService = methodModelParameter.getCurrentService();
            if(currentService == null || !methodModelParameter.isPossibleService()) {
                currentService = "?";
            }

            methodCalls.add(currentService);

        }

        if(!hasCall || methodCalls.isEmpty()) {
            return null;
        }

        return methodCalls;
    }

    @Nullable
    private String buildXml(Map<String, List<MethodParameter.MethodModelParameter>> methods, String className, String serviceName) {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            return null;
        }

        // root elements
        final Document doc = docBuilder.newDocument();
        final Element rootElement = doc.createElement("service");
        rootElement.setAttribute("id", !this.isClassAsIdAttribute ? serviceName : className);

        if(!this.isClassAsIdAttribute) {
            rootElement.setAttribute("class", className);
        }

        doc.appendChild(rootElement);

        if(methods.containsKey("__construct")) {

            List<String> parameters = getParameters(methods.get("__construct"));
            if(parameters != null) {
                appendArgumentXmlTags(doc, rootElement, parameters);
            }

            methods.remove("__construct");
        }

        for(Map.Entry<String, List<MethodParameter.MethodModelParameter>> entry: methods.entrySet()) {

            List<String> parameters = getParameters(entry.getValue());
            if(parameters != null) {
                Element calls = doc.createElement("call");
                calls.setAttribute("method", entry.getKey());
                appendArgumentXmlTags(doc, calls, parameters);
                rootElement.appendChild(calls);
            }
        }

        serviceTagCallback(className, serviceTags -> {

            for (ServiceTag serviceTag : serviceTags) {
                try {
                    // convert string to node
                    Element node = DocumentBuilderFactory
                        .newInstance()
                        .newDocumentBuilder()
                        .parse(new ByteArrayInputStream(serviceTag.toXmlString().getBytes()))
                        .getDocumentElement();

                    rootElement.appendChild(doc.importNode(node, true));

                } catch (SAXException | IOException | ParserConfigurationException ignored) {
                }
            }
        });


        try {
            return getStringFromDocument(doc);
        } catch (TransformerException e) {
            return null;
        }
    }

    /**
     * Build arguments tags
     *
     * <argument type="service" id="foobar"/>
     */
    private void appendArgumentXmlTags(@NotNull Document doc, @NotNull Element rootElement, @NotNull List<String> parameters) {
        for(String parameter: parameters) {
            Element argument = doc.createElement("argument");
            argument.setAttribute("type", "service");
            argument.setAttribute("id", parameter);
            rootElement.appendChild(argument);
        }
    }

    private static String getStringFromDocument(Document doc) throws TransformerException {

        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        transformer.transform(new DOMSource(doc), result);

        return writer.toString();
    }


    private String buildYaml(Map<String, List<MethodParameter.MethodModelParameter>> methods, String className, String serviceName) {
        int indentSpaces = 4;

        // find indent on file content
        if(psiFile instanceof YAMLFile) {
            indentSpaces = YamlHelper.getIndentSpaceForFile((YAMLFile) psiFile);
        }

        // yaml files are spaces only; fill indent
        String indent = StringUtils.repeat(" ", indentSpaces);

        List<String> lines = new ArrayList<>();

        lines.add((this.isClassAsIdAttribute ? className : serviceName) + ":");
        if(!this.isClassAsIdAttribute) {
            lines.add(indent + "class: " + className);
        }

        if(methods.containsKey("__construct")) {
            List<String> parameters = getParameters(methods.get("__construct"));
            if(parameters != null) {
                lines.add(String.format("%sarguments: [%s]", indent, StringUtils.join(formatYamlService(parameters), ", ")));
            }

            methods.remove("__construct");
        }

        List<String> calls = new ArrayList<>();
        for(Map.Entry<String, List<MethodParameter.MethodModelParameter>> entry: methods.entrySet()) {
            List<String> parameters = getParameters(entry.getValue());
            if(parameters != null) {
                calls.add(String.format("%s%s- [%s, [%s]]", indent, indent, entry.getKey(), StringUtils.join(formatYamlService(parameters), ", ")));
            }
        }

        if(!calls.isEmpty()) {
            lines.add(indent + "calls:");
            lines.addAll(calls);
        }

        serviceTagCallback(className, serviceTags -> {
            lines.add(indent + "tags:");
            for (ServiceTag serviceTag : serviceTags) {
                lines.add(indent + indent + serviceTag.toYamlString());
            }
        });

        if(lines.size() == 1) {
            lines.set(0, lines.get(0) + " ~");
        }

        return StringUtils.join(lines, "\n");
    }

    private void serviceTagCallback(String className, TagCallbackInterface callback) {

        PhpClass phpClass = PhpElementsUtil.getClass(project, className);
        if(phpClass == null) {
            return;
        }

        List<ServiceTag> serviceTags = new ArrayList<>();
        for (String tag : ServiceUtil.getPhpClassServiceTags(phpClass)) {
            ServiceTag serviceTag = new ServiceTag(phpClass, tag);
            ServiceUtil.decorateServiceTag(serviceTag);
            serviceTags.add(serviceTag);
        }

        if(serviceTags.isEmpty()) {
            return;
        }

        callback.onTags(serviceTags);
    }

    private List<String> formatYamlService(List<String> parameters) {

        // append yaml syntax, more will follow...
        List<String> yamlSyntaxParameters = new ArrayList<>();
        for(String parameter: parameters) {
            yamlSyntaxParameters.add(String.format("'@%s'", parameter));
        }

        return yamlSyntaxParameters;
    }

    public interface TagCallbackInterface {
        void onTags(@NotNull List<ServiceTag> tags);
    }
}
