package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.NewExpressionImpl;
import com.jetbrains.php.phpunit.PhpUnitUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtension;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwigExtensionParser  {

    private Project project;

    private HashMap<String, TwigExtension> functions;
    private HashMap<String, TwigExtension> filters;

    public TwigExtensionParser(Project project) {
        this.project = project;
    }

    public HashMap<String, TwigExtension> getFunctions() {
        if(functions == null) {
            this.parseElementType(TwigElementType.METHOD);
        }
        return functions;
    }

    public HashMap<String, TwigExtension> getFilters() {
        if(filters == null) {
            this.parseElementType(TwigElementType.FILTER);
        }
        return filters;
    }

    public enum TwigElementType {
        FILTER, METHOD
    }

    public enum TwigExtensionType {
        FUNCTION_METHOD, FUNCTION_NODE, SIMPLE_FUNCTION, FILTER
    }

    private void parseElementType(TwigElementType type) {


        Set<String> classNames = new HashSet<String>();

        // only the interface gaves use all elements; container dont hold all
        for(PhpClass phpClass : PhpIndex.getInstance(this.project).getAllSubclasses("\\Twig_ExtensionInterface")) {
            // dont add unit tests classes
            if(!PhpUnitUtil.isPhpUnitTestFile(phpClass.getContainingFile())) {
                String className = phpClass.getPresentableFQN();
                if(className != null) {
                    classNames.add(className);
                }
            }
        }

        if(type.equals(TwigElementType.FILTER)) {
            this.parseFilters(classNames);
        }

        if(type.equals(TwigElementType.METHOD)) {
            this.parseFunctions(classNames);
        }

    }

    private void parseFilters(Collection<String> classNames) {
        this.filters = new HashMap<String, TwigExtension>();
        for(String phpClassName : classNames) {
            Method method = PhpElementsUtil.getClassMethod(this.project, phpClassName, "getFilters");
            if(method != null) {
                parseFilter(method, this.filters);
            }
        }
    }

    private void parseFunctions(Collection<String> classNames) {
        this.functions = new HashMap<String, TwigExtension>();
        for(String phpClassName : classNames) {
            Method method = PhpElementsUtil.getClassMethod(this.project, phpClassName, "getFunctions");
            if(method != null) {
                parseFunctions(method, this.functions);
            }
        }
    }

    protected Map<String, TwigExtension> parseFunctions(final Method method, final HashMap<String, TwigExtension> filters) {

        final PhpClass containingClass = method.getContainingClass();
        if(containingClass == null) {
            return new HashMap<String, TwigExtension>();
        }

        method.acceptChildren(new TwigFunctionVisitor(method, filters, containingClass));

        return filters;

    }

    /**
     *  Get signature for callable like array($this, 'getUrl'), or 'function'
     */
    @Nullable
    private String getCallableSignature(PsiElement psiElement, Method method) {

        // array($this, 'getUrl')
        if(psiElement instanceof ArrayCreationExpression) {
            List<PsiElement> arrayValues = (List<PsiElement>) PsiElementUtils.getChildrenOfTypeAsList(psiElement, PlatformPatterns.psiElement(PhpElementTypes.ARRAY_VALUE));
            if(arrayValues.size() > 1) {
                PsiElement firstChild = arrayValues.get(0).getFirstChild();
                if(firstChild instanceof Variable && "this".equals(((Variable) firstChild).getName())) {
                    String methodName = PhpElementsUtil.getStringValue(arrayValues.get(1).getFirstChild());
                    if(StringUtils.isNotBlank(methodName)) {
                        PhpClass phpClass = method.getContainingClass();
                        if(phpClass != null) {
                            return String.format("#M#C\\%s.%s", phpClass.getPresentableFQN(), methodName);
                        }
                    }
                }
            }

        } else {
            String funcTargetName = PhpElementsUtil.getStringValue(psiElement);
            if(funcTargetName != null) {
                return "#F" + funcTargetName;
            }
        }

        return null;
    }

    protected HashMap<String, TwigExtension> parseFilter(Method method, HashMap<String, TwigExtension> filters) {

        String text = method.getText();

        Matcher simpleFilter = Pattern.compile("[\\\\]*(Twig_SimpleFilter)[\\s+]*\\([\\s+]*['\"](.*?)['\"][\\s+]*,[\\s+]*['\"](.*?)['\"][\\s+]*").matcher(text);
        while(simpleFilter.find()){
            if(simpleFilter.group(1).matches("\\w+")) {
                filters.put(simpleFilter.group(2), new TwigExtension(TwigExtensionType.FILTER, "#F" + PsiElementUtils.trimQuote(simpleFilter.group(3).trim())));
            }
        }


        Matcher filterFunction = Pattern.compile("['\"](.*?)['\"][\\s+]*=>[\\s+]*new[\\s+]*[\\\\]*(Twig_Filter_Function)\\((.*?),").matcher(text);
        while(filterFunction.find()){
            if(filterFunction.group(1).matches("\\w+")) {
                filters.put(filterFunction.group(1), new TwigExtension(TwigExtensionType.FILTER, "#F" + PsiElementUtils.trimQuote(filterFunction.group(3).trim())));
            }
        }

        Matcher filterMethod = Pattern.compile("['\"](.*?)['\"][\\s+]*=>[\\s+]*new[\\s+]*[\\\\]*(Twig_Filter_Method)\\((.*?),(.*?)[,|)]").matcher(text);
        while(filterMethod.find()){

            // we have "$" and also "*_path" test case only?
            if(filterMethod.group(1).matches("\\w+")) {
                filters.put(filterMethod.group(1), new TwigExtension(TwigExtensionType.FILTER, findThisMethod(method, filterMethod)));
            }

        }

        return filters;

    }

    private String findThisMethod(Method method, Matcher filterMethod) {
        String signature = null;
        if(filterMethod.group(3).trim().equals("$this")) {
            PhpClass phpClass = method.getContainingClass();
            if(phpClass != null) {
                signature = "#M#C\\" + phpClass.getPresentableFQN() + "." + PsiElementUtils.trimQuote(filterMethod.group(4).trim());
            }
        }
        return signature;
    }

    public static Icon getIcon(TwigExtensionType twigExtensionType) {

        if(twigExtensionType == TwigExtensionType.FUNCTION_NODE) {
            return PhpIcons.CLASS_INITIALIZER;
        }

        if(twigExtensionType == TwigExtensionType.SIMPLE_FUNCTION) {
            return PhpIcons.FUNCTION;
        }

        if(twigExtensionType == TwigExtensionType.FUNCTION_METHOD) {
            return PhpIcons.METHOD_ICON;
        }

        if(twigExtensionType == TwigExtensionType.FILTER) {
            return PhpIcons.STATIC_FIELD;
        }

        return PhpIcons.WEB_ICON;
    }

    private class TwigFunctionVisitor extends PsiRecursiveElementWalkingVisitor {
        private final Method method;
        private final HashMap<String, TwigExtension> filters;
        private final PhpClass containingClass;

        public TwigFunctionVisitor(Method method, HashMap<String, TwigExtension> filters, PhpClass containingClass) {
            this.method = method;
            this.filters = filters;
            this.containingClass = containingClass;
        }

        @Override
        public void visitElement(PsiElement element) {
            if(element instanceof NewExpressionImpl) {
                this.visitNewExpression((NewExpressionImpl) element);
            }
            super.visitElement(element);
        }

        private void visitNewExpression(NewExpressionImpl element) {

            ClassReference classReference = element.getClassReference();
            if(classReference == null) {
                return;
            }

            String expressionName = classReference.getName();

            // new \Twig_SimpleFunction('url', array($this, 'getUrl'), array('is_safe_callback' => array($this, 'isUrlGenerationSafe'))),
            if("Twig_SimpleFunction".equals(expressionName)) {
                PsiElement[] psiElement = element.getParameters();
                if(psiElement.length > 0) {
                    String funcName = PhpElementsUtil.getStringValue(psiElement[0]);
                    if(funcName != null && !funcName.contains("*")) {

                        String signature = null;
                        if(psiElement.length > 1) {
                            signature = getCallableSignature(psiElement[1], method);
                        }

                        filters.put(funcName,  new TwigExtension(TwigExtensionType.SIMPLE_FUNCTION, signature));

                    }

                }

                return;
            }

            //array('form_javascript' => new \Twig_Function_Method($this, 'renderJavascript', array('is_safe' => array('html'))),);
            if("Twig_Function_Method".equals(expressionName)) {
                PsiElement arrayValue = element.getParent();
                if(arrayValue != null && arrayValue.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE) {
                    PsiElement arrayHash = arrayValue.getParent();
                    if(arrayHash instanceof ArrayHashElement) {
                        PsiElement arrayKey = ((ArrayHashElement) arrayHash).getKey();
                        String funcName = PhpElementsUtil.getStringValue(arrayKey);
                        if(funcName != null && !funcName.contains("*")) {

                            PsiElement[] parameters = element.getParameters();
                            String signature = null;
                            if(parameters.length > 1) {
                                if(parameters[0] instanceof Variable && "this".equals(((Variable) parameters[0]).getName())) {
                                    String methodName = PhpElementsUtil.getStringValue(parameters[1]);
                                    if(methodName != null) {
                                        String presentableFQN = containingClass.getPresentableFQN();
                                        if(presentableFQN != null) {
                                            signature = String.format("#M#C\\%s.%s", presentableFQN, methodName);
                                        }
                                    }

                                }
                            }

                            filters.put(funcName, new TwigExtension(TwigExtensionType.FUNCTION_METHOD, signature));
                        }
                    }
                }

                return;
            }

            // array('form_help' => new \Twig_Function_Node('Symfony\Bridge\Twig\Node\SearchAndRenderBlockNode', array('is_safe' => array('html'))),)
            if("Twig_Function_Node".equals(expressionName)) {
                PsiElement arrayValue = element.getParent();
                if(arrayValue != null && arrayValue.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE) {
                    PsiElement arrayHash = arrayValue.getParent();
                    if(arrayHash instanceof ArrayHashElement) {
                        PsiElement arrayKey = ((ArrayHashElement) arrayHash).getKey();
                        String funcName = PhpElementsUtil.getStringValue(arrayKey);
                        if(funcName != null && !funcName.contains("*")) {

                            PsiElement[] parameters = element.getParameters();
                            String signature = null;
                            if(parameters.length > 0) {
                                String className = PhpElementsUtil.getStringValue(parameters[1]);
                                if(className != null) {

                                    if(!className.startsWith("\\")) {
                                        className = className.substring(1);
                                    }

                                    signature = String.format("#M#C\\%s.%s", className, "compile");
                                }

                            }

                            filters.put(funcName, new TwigExtension(TwigExtensionType.FUNCTION_NODE, signature));
                        }
                    }
                }

            }

        }
    }
}
