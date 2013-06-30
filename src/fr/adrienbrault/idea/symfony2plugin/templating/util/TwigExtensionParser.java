package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwigExtensionParser  {

    private Project project;

    private HashMap<String, String> functions;
    private HashMap<String, String> filters;

    public TwigExtensionParser(Project project) {
        this.project = project;
    }

    public HashMap<String, String> getFunctions() {
        if(filters == null) {
            this.parseElementType(TwigElementType.METHOD);
        }
        return functions;
    }

    public HashMap<String, String> getFilters() {
        if(filters == null) {
            this.parseElementType(TwigElementType.FILTER);
        }
        return filters;
    }

    public enum TwigElementType {
        FILTER, METHOD
    }

    private void parseElementType(TwigElementType type) {

        // only the interface gaves use all elements; container dont hold all
        PhpIndex phpIndex = PhpIndex.getInstance(this.project);
        ArrayList<String> classNames = new ArrayList<String>();
        for(PhpClass phpClass : phpIndex.getAllSubclasses("\\Twig_ExtensionInterface")) {
            String className = phpClass.getPresentableFQN();
            if(className != null) {
                // signature class names need slash at first
                classNames.add(className.startsWith("\\") ? className : "\\" + className);
            }
        }

        if(type.equals(TwigElementType.FILTER)) {
            this.parseFilters(classNames);
        }

        if(type.equals(TwigElementType.METHOD)) {
            this.parseFunctions(classNames);
        }

    }

    private void parseFilters(ArrayList<String> classNames) {
        this.filters = new HashMap<String, String>();
        for(String phpClassName : classNames) {
            PhpIndex phpIndex = PhpIndex.getInstance(this.project);
            Collection<? extends PhpNamedElement> phpNamedElementCollections = phpIndex.getBySignature("#M#C" + phpClassName + "." + "getFilters", null, 0);
            for(PhpNamedElement phpNamedElement: phpNamedElementCollections) {
                if(phpNamedElement instanceof Method) {
                    parseFilter(phpNamedElement.getText(), this.filters);
                }
            }
        }
    }

    private void parseFunctions(ArrayList<String> classNames) {
        this.functions = new HashMap<String, String>();
        for(String phpClassName : classNames) {

            PhpIndex phpIndex = PhpIndex.getInstance(this.project);
            Collection<? extends PhpNamedElement> phpNamedElementCollections = phpIndex.getBySignature("#M#C" + phpClassName + "." + "getFunctions", null, 0);
            for(PhpNamedElement phpNamedElement: phpNamedElementCollections) {
                if(phpNamedElement instanceof Method) {
                    parseFunctions(phpNamedElement.getText(), this.functions);
                }
            }
        }
    }

    protected HashMap<String, String> parseFunctions(String text, HashMap<String, String> filters) {

        Matcher simpleFunction = Pattern.compile("[\\\\]*(Twig_SimpleFunction)[\\s+]*\\(['\"](.*?)['\"][\\s+]*").matcher(text);
        while(simpleFunction.find()){
            if(!simpleFunction.group(2).contains("*")) {
                filters.put(simpleFunction.group(2), simpleFunction.group(1));
            }
        }


        Matcher filterFunction = Pattern.compile("['\"](.*?)['\"][\\s+]*=>[\\s+]*new[\\s+]*[\\\\]*(Twig_Function_Method|Twig_Function_Node)").matcher(text);
        while(filterFunction.find()){
            if(!filterFunction.group(1).contains("*")) {
                filters.put(filterFunction.group(1), filterFunction.group(2));
            }
        }

        return filters;

    }

    protected HashMap<String, String> parseFilter(String text, HashMap<String, String> filters) {

        Matcher simpleFilter = Pattern.compile("[\\\\]*(Twig_SimpleFilter)[\\s+]*\\(['\"](.*?)['\"][\\s+]*").matcher(text);
        while(simpleFilter.find()){
            if(!simpleFilter.group(2).contains("*")) {
                filters.put(simpleFilter.group(2), simpleFilter.group(1));
            }
        }


        Matcher filterFunction = Pattern.compile("['\"](.*?)['\"][\\s+]*=>[\\s+]*new[\\s+]*[\\\\]*(Twig_Filter_Function)").matcher(text);
        while(filterFunction.find()){
            if(!filterFunction.group(1).contains("*")) {
                filters.put(filterFunction.group(1), filterFunction.group(2));
            }
        }

        return filters;

    }

}
