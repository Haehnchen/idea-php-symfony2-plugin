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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

public class TwigExtensionParser  {

    private Project project;

    private Map<String, TwigExtension> functions;
    private Map<String, TwigExtension> simpleTest;
    private Map<String, TwigExtension> filters;

    public TwigExtensionParser(Project project) {
        this.project = project;
    }

    public Map<String, TwigExtension> getFunctions() {
        if(functions == null) {
            this.parseElementType(TwigElementType.METHOD);
        }
        return functions;
    }

    public Map<String, TwigExtension> getFilters() {
        if(filters == null) {
            this.parseElementType(TwigElementType.FILTER);
        }
        return filters;
    }

    public Map<String, TwigExtension> getSimpleTest() {
        if(simpleTest == null) {
            this.parseElementType(TwigElementType.SIMPLE_TEST);
        }
        return simpleTest;
    }

    public enum TwigElementType {
        FILTER, METHOD, SIMPLE_TEST
    }

    public enum TwigExtensionType {
        FUNCTION_METHOD, FUNCTION_NODE, SIMPLE_FUNCTION, FILTER, SIMPLE_TEST
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

        if(type.equals(TwigElementType.SIMPLE_TEST)) {
            this.parseTests(classNames);
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

    private void parseTests(Collection<String> classNames) {
        this.simpleTest = new HashMap<String, TwigExtension>();
        for(String phpClassName : classNames) {
            Method method = PhpElementsUtil.getClassMethod(this.project, phpClassName, "getTests");
            if(method != null) {
                parseSimpleTest(method, this.simpleTest);
            }
        }
    }

    protected Map<String, TwigExtension> parseFunctions(final Method method, final Map<String, TwigExtension> filters) {

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
    private static String getCallableSignature(PsiElement psiElement, Method method) {

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

                if(funcTargetName.contains("::")) {
                    // 'SqlFormatter::format'
                    String[] splits = funcTargetName.split("::");
                    if(splits.length >= 2) {
                        return String.format("#M#C\\%s.%s", splits[0], splits[1]);
                    }
                } else {
                    return "#F" + funcTargetName;
                }
            }
        }

        return null;
    }

    protected Map<String, TwigExtension> parseFilter(Method method, Map<String, TwigExtension> filters) {


        final PhpClass containingClass = method.getContainingClass();
        if(containingClass == null) {
            return new HashMap<String, TwigExtension>();
        }

        method.acceptChildren(new TwigFilterVisitor(method, filters, containingClass));

        return filters;

    }

    protected Map<String, TwigExtension> parseSimpleTest(Method method, Map<String, TwigExtension> filters) {


        final PhpClass containingClass = method.getContainingClass();
        if(containingClass == null) {
            return new HashMap<String, TwigExtension>();
        }

        method.acceptChildren(new TwigSimpleTestVisitor(method, filters, containingClass));

        return filters;

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

        if(twigExtensionType == TwigExtensionType.SIMPLE_TEST) {
            return PhpIcons.CONSTANT;
        }

        return PhpIcons.WEB_ICON;
    }
    private static class TwigFilterVisitor extends PsiRecursiveElementWalkingVisitor {
        private final Method method;
        private final Map<String, TwigExtension> filters;
        private final PhpClass containingClass;

        public TwigFilterVisitor(Method method, Map<String, TwigExtension> filters, PhpClass containingClass) {
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
            if("Twig_SimpleFilter".equals(expressionName)) {
                PsiElement[] psiElement = element.getParameters();
                if(psiElement.length > 0) {
                    String funcName = PhpElementsUtil.getStringValue(psiElement[0]);
                    if(funcName != null && !funcName.contains("*")) {

                        String signature = null;
                        if(psiElement.length > 1) {
                            signature = getCallableSignature(psiElement[1], method);
                        }

                        TwigExtension twigExtension = new TwigExtension(TwigExtensionType.FILTER, signature);

                        if(psiElement.length > 2 && psiElement[2] instanceof ArrayCreationExpression) {
                            decorateOptions((ArrayCreationExpression) psiElement[2], twigExtension);
                        }

                        filters.put(funcName, twigExtension);
                    }

                }

                return;
            }

            // array('shuffle' => new Twig_Filter_Function('twig_shuffle_filter'),)
            if("Twig_Filter_Function".equals(expressionName)) {
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
                                signature = getCallableSignature(parameters[0], method);
                            }

                            filters.put(funcName, new TwigExtension(TwigExtensionType.FILTER, signature));
                        }
                    }
                }

                return;
            }

            // return array('serialize'  => new \Twig_Filter_Method($this, 'serialize'), );
            if("Twig_Filter_Method".equals(expressionName)) {
                PsiElement arrayValue = element.getParent();
                if(arrayValue != null && arrayValue.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE) {
                    PsiElement arrayHash = arrayValue.getParent();
                    if(arrayHash instanceof ArrayHashElement) {
                        PsiElement arrayKey = ((ArrayHashElement) arrayHash).getKey();
                        String funcName = PhpElementsUtil.getStringValue(arrayKey);
                        if(funcName != null && funcName.matches("\\w+")) {

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

                            filters.put(funcName, new TwigExtension(TwigExtensionType.FILTER, signature));
                        }
                    }
                }

            }

        }
    }

    /**
     * Add needs_environment, needs_context values to twig extension object
     */
    private static void decorateOptions(@NotNull ArrayCreationExpression arrayCreationExpression, @NotNull TwigExtension twigExtension) {
        for(String optionTrue: new String[] {"needs_environment", "needs_context"}) {
            PhpPsiElement phpPsiElement = PhpElementsUtil.getArrayValue(arrayCreationExpression, optionTrue);
            if(phpPsiElement instanceof ConstantReference) {
                String value = phpPsiElement.getName();
                if(value != null && value.toLowerCase().equals("true")) {
                    twigExtension.putOption(optionTrue, "true");
                }
            }
        }
    }

    private static class TwigFunctionVisitor extends PsiRecursiveElementWalkingVisitor {
        private final Method method;
        private final Map<String, TwigExtension> filters;
        private final PhpClass containingClass;

        public TwigFunctionVisitor(Method method, Map<String, TwigExtension> filters, PhpClass containingClass) {
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

                        TwigExtension twigExtension = new TwigExtension(TwigExtensionType.SIMPLE_FUNCTION, signature);
                        if(psiElement.length > 2 && psiElement[2] instanceof ArrayCreationExpression) {
                            decorateOptions((ArrayCreationExpression) psiElement[2], twigExtension);
                        }

                        filters.put(funcName, twigExtension);

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
                                String className = PhpElementsUtil.getStringValue(parameters[0]);
                                if(className != null) {

                                    if(className.startsWith("\\")) {
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

    private static class TwigSimpleTestVisitor extends PsiRecursiveElementWalkingVisitor {
        private final Method method;
        private final Map<String, TwigExtension> filters;
        private final PhpClass containingClass;

        public TwigSimpleTestVisitor(Method method, Map<String, TwigExtension> filters, PhpClass containingClass) {
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

            // new Twig_SimpleTest('even', null, array('node_class' => 'Twig_Node_Expression_Test_Even')),
            if("Twig_SimpleTest".equals(expressionName)) {
                PsiElement[] psiElement = element.getParameters();
                if(psiElement.length > 0) {
                    String funcName = PhpElementsUtil.getStringValue(psiElement[0]);
                    if(funcName != null && !funcName.contains("*")) {
                        filters.put(funcName, new TwigExtension(TwigExtensionType.SIMPLE_TEST, null));
                    }

                }
            }

        }
    }

}
