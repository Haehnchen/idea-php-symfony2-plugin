package fr.adrienbrault.idea.symfony2plugin.action.ui;

import com.jetbrains.php.lang.psi.elements.Method;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.*;

public class ServiceBuilder {

    public enum OutputType {
        Yaml, XML,
    }

    private List<MethodParameter.MethodModelParameter>  methodModelParameter;

    public ServiceBuilder(List<MethodParameter.MethodModelParameter> methodModelParameter) {
        this.methodModelParameter = methodModelParameter;
    }

    @Nullable
    public String build(OutputType outputType, String className) {
        HashMap<String, ArrayList<MethodParameter.MethodModelParameter>> methods = new HashMap<String, ArrayList<MethodParameter.MethodModelParameter>>();

        for(MethodParameter.MethodModelParameter methodModelParameter: this.methodModelParameter) {

            String methodName = methodModelParameter.getName();
            if(methodModelParameter.getMethod().getMethodType(false) == Method.MethodType.CONSTRUCTOR) {
                methodName = "__construct";
            }

            if(methods.containsKey(methodModelParameter.getName())) {
                methods.get(methodName).add(methodModelParameter);
            } else {
                methods.put(methodName, new ArrayList<MethodParameter.MethodModelParameter>(Arrays.asList(methodModelParameter)));
            }
        }

        if(outputType == OutputType.Yaml) {
            return buildYaml(methods, className);
        }

        if(outputType == OutputType.XML) {
            return buildXml(methods, className);
        }

        return null;
    }

    @Nullable
    private List<String> getParameters(List<MethodParameter.MethodModelParameter> methodModelParameters) {
        boolean hasCall = false;
        ArrayList<String> methodCalls = new ArrayList<String>();

        // sort by indexes parameter
        Collections.sort(methodModelParameters, new Comparator<MethodParameter.MethodModelParameter>() {
            @Override
            public int compare(MethodParameter.MethodModelParameter o1, MethodParameter.MethodModelParameter o2) {
                return ((Integer) o1.getIndex()).compareTo(o2.getIndex());
            }
        });

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

        if(!hasCall || methodCalls.size() == 0) {
            return null;
        }

        return methodCalls;
    }

    @Nullable
    private String buildXml(Map<String, ArrayList<MethodParameter.MethodModelParameter>> methods, String className) {

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = null;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            return null;
        }


        // root elements
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("service");
        rootElement.setAttribute("id", generateServiceName(className));
        rootElement.setAttribute("class", className);
        doc.appendChild(rootElement);

        if(methods.containsKey("__construct")) {

            List<String> parameters = getParameters(methods.get("__construct"));
            if(parameters != null) {
                for(String parameter: parameters) {
                    Element argument = doc.createElement("argument");
                    argument.setAttribute("id", parameter);
                    argument.setAttribute("type", "service");
                    rootElement.appendChild(argument);
                }
            }

            methods.remove("__construct");
        }


        for(Map.Entry<String, ArrayList<MethodParameter.MethodModelParameter>> entry: methods.entrySet()) {

            List<String> parameters = getParameters(entry.getValue());
            if(parameters != null) {
                Element calls = doc.createElement("call");
                calls.setAttribute("method", entry.getKey());

                for(String parameter: parameters) {
                    Element argument = doc.createElement("argument");
                    argument.setAttribute("id", parameter);
                    argument.setAttribute("type", "service");
                    calls.appendChild(argument);
                }

                rootElement.appendChild(calls);
            }
        }


        try {
            return getStringFromDocument(doc);
        } catch (TransformerException e) {
            return null;
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


    private String buildYaml(Map<String, ArrayList<MethodParameter.MethodModelParameter>> methods, String className) {
        String out = "";

        out += generateServiceName(className) + ":\n";
        out += "  class: " + className + "\n";

        if(methods.containsKey("__construct")) {

            List<String> parameters = getParameters(methods.get("__construct"));
            if(parameters != null) {
                out += String.format("  arguments: [ %s ]", StringUtils.join(parameters, ", ")) + "\n";
            }

            methods.remove("__construct");
        }

        String calls = "";
        for(Map.Entry<String, ArrayList<MethodParameter.MethodModelParameter>> entry: methods.entrySet()) {
            List<String> parameters = getParameters(entry.getValue());
            if(parameters != null) {

                // append yaml syntax, more will follow...
                List<String> yamlSyntaxParameters = new ArrayList<String>();
                for(String parameter: parameters) {
                    yamlSyntaxParameters.add("@" + parameter);
                }

                calls += String.format("    - [ %s, [ %s ] ]", entry.getKey(), StringUtils.join(yamlSyntaxParameters, ", ")) + "\n";
            }
        }

        if(!StringUtils.isBlank(calls)) {
            out+= "  calls:\n" + calls;
        }

        return out;
    }

    private String generateServiceName(String className) {

        if(className.contains("Bundle")) {
            String formattedName = className.substring(0, className.indexOf("Bundle") + 6).toLowerCase().replace("\\", "_");
            formattedName += className.substring(className.indexOf("Bundle") + 6).toLowerCase().replace("\\", ".");
            return formattedName;
        }

        return className.toLowerCase().replace("\\", "_");
    }

}
