package fr.adrienbrault.idea.symfony2plugin.action.ui;

import com.jetbrains.php.lang.psi.elements.Method;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ServiceBuilder {

    public enum OutputType {
        Yaml, XML,
    }

    private List<MethodParameter.MethodModelParameter>  methodModelParameter;

    public ServiceBuilder(List<MethodParameter.MethodModelParameter> methodModelParameter) {
        this.methodModelParameter = methodModelParameter;
    }

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

        return "";
    }

    @Nullable
    private List<String> getParameters(List<MethodParameter.MethodModelParameter> methodModelParameters) {
        boolean hasCall = false;
        ArrayList<String> methodCalls = new ArrayList<String>();

        // sort by index parameter
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

            String currentService = methodModelParameter.getCurrentService();
            if(currentService == null || !methodModelParameter.isPossibleService()) {
                currentService = "?";
            }

            methodCalls.add("@" + currentService);

        }

        if(!hasCall || methodCalls.size() == 0) {
            return null;
        }

        return methodCalls;
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
                calls += String.format("    - [ %s, [ %s ] ]", entry.getKey(), StringUtils.join(parameters, ", ")) + "\n";
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
