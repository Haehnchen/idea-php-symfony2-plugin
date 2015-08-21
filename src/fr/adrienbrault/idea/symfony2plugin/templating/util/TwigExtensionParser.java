package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Key;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.util.*;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
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

    private static final Key<CachedValue<Map<String, TwigExtension>>> FUNCTION_CACHE = new Key<CachedValue<Map<String, TwigExtension>>>("TWIG_EXTENSIONS_FUNCTION");
    private static final Key<CachedValue<Map<String, TwigExtension>>> TEST_CACHE = new Key<CachedValue<Map<String, TwigExtension>>>("TWIG_EXTENSIONS_TEST");
    private static final Key<CachedValue<Map<String, TwigExtension>>> FILTERS_CACHE = new Key<CachedValue<Map<String, TwigExtension>>>("TWIG_EXTENSIONS_FILTERS");
    private static final Key<CachedValue<Map<String, TwigExtension>>> OPERATORS_CACHE = new Key<CachedValue<Map<String, TwigExtension>>>("TWIG_EXTENSIONS_OPERATORS");

    private Project project;

    private Map<String, TwigExtension> functions;
    private Map<String, TwigExtension> simpleTest;
    private Map<String, TwigExtension> filters;
    private Map<String, TwigExtension> operators;

    public TwigExtensionParser(@NotNull Project project) {
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

    public Map<String, TwigExtension> getOperators() {
        if(operators == null) {
            this.parseElementType(TwigElementType.OPERATOR);
        }
        return operators;
    }

    public enum TwigElementType {
        FILTER, METHOD, SIMPLE_TEST, OPERATOR
    }

    public enum TwigExtensionType {
        FUNCTION_METHOD, FUNCTION_NODE, SIMPLE_FUNCTION, FILTER, SIMPLE_TEST, OPERATOR
    }

    private void parseElementType(TwigElementType type) {

        if(type.equals(TwigElementType.FILTER)) {

            CachedValue<Map<String, TwigExtension>> cache = project.getUserData(FILTERS_CACHE);
            if(cache == null) {
                cache = CachedValuesManager.getManager(project).createCachedValue(new CachedValueProvider<Map<String, TwigExtension>>() {
                    @Nullable
                    @Override
                    public Result<Map<String, TwigExtension>> compute() {
                        return Result.create(parseFilters(getTwigExtensionClasses()), PsiModificationTracker.MODIFICATION_COUNT);
                    }
                }, false);

                project.putUserData(FILTERS_CACHE, cache);
            }

            this.filters = cache.getValue();

        } else if(type.equals(TwigElementType.METHOD)) {

            CachedValue<Map<String, TwigExtension>> cache = project.getUserData(FUNCTION_CACHE);
            if(cache == null) {
                cache = CachedValuesManager.getManager(project).createCachedValue(new CachedValueProvider<Map<String, TwigExtension>>() {
                    @Nullable
                    @Override
                    public Result<Map<String, TwigExtension>> compute() {
                        return Result.create(parseFunctions(getTwigExtensionClasses()), PsiModificationTracker.MODIFICATION_COUNT);
                    }
                }, false);

                project.putUserData(FUNCTION_CACHE, cache);
            }

            this.functions = cache.getValue();

        } else if(type.equals(TwigElementType.SIMPLE_TEST)) {

            CachedValue<Map<String, TwigExtension>> cache = project.getUserData(TEST_CACHE);
            if(cache == null) {
                cache = CachedValuesManager.getManager(project).createCachedValue(new CachedValueProvider<Map<String, TwigExtension>>() {
                    @Nullable
                    @Override
                    public Result<Map<String, TwigExtension>> compute() {
                        return Result.create(parseTests(getTwigExtensionClasses()), PsiModificationTracker.MODIFICATION_COUNT);
                    }
                }, false);

                project.putUserData(TEST_CACHE, cache);
            }

            this.simpleTest = cache.getValue();

        } else if(type.equals(TwigElementType.OPERATOR)) {

            CachedValue<Map<String, TwigExtension>> cache = project.getUserData(OPERATORS_CACHE);
            if(cache == null) {
                cache = CachedValuesManager.getManager(project).createCachedValue(new CachedValueProvider<Map<String, TwigExtension>>() {
                    @Nullable
                    @Override
                    public Result<Map<String, TwigExtension>> compute() {
                        return Result.create(parseOperators(getTwigExtensionClasses()), PsiModificationTracker.MODIFICATION_COUNT);
                    }
                }, false);

                project.putUserData(OPERATORS_CACHE, cache);
            }

            this.operators = cache.getValue();
        }
    }

    private Collection<PhpClass> getTwigExtensionClasses() {

        Collection<PhpClass> phpClasses = new ArrayList<PhpClass>();

        // only the interface gave use all elements; service container dont hold all
        for(PhpClass phpClass : PhpIndex.getInstance(this.project).getAllSubclasses("\\Twig_ExtensionInterface")) {

            // dont add unit tests classes
            if(PhpUnitUtil.isPhpUnitTestFile(phpClass.getContainingFile())) {
                continue;
            }

            phpClasses.add(phpClass);
        }

        return phpClasses;
    }

    private Map<String, TwigExtension> parseFilters(Collection<PhpClass> phpClasses) {
        Map<String, TwigExtension> extensions = new HashMap<String, TwigExtension>();
        for(PhpClass phpClass : phpClasses) {
            Method method = phpClass.findMethodByName("getFilters");
            if(method != null) {
                parseFilter(method, extensions);
            }
        }

        return extensions;
    }

    private Map<String, TwigExtension> parseFunctions(Collection<PhpClass> phpClasses) {
        Map<String, TwigExtension> extensions = new HashMap<String, TwigExtension>();
        for(PhpClass phpClass : phpClasses) {
            Method method = phpClass.findMethodByName("getFunctions");
            if(method != null) {
                parseFunctions(method, extensions);
            }
        }

        return extensions;
    }

    private Map<String, TwigExtension> parseTests(Collection<PhpClass> phpClasses) {
        Map<String, TwigExtension> extensions = new HashMap<String, TwigExtension>();
        for(PhpClass phpClass : phpClasses) {
            Method method = phpClass.findMethodByName("getTests");
            if(method != null) {
                parseSimpleTest(method, extensions);
            }
        }

        return extensions;
    }

    private Map<String, TwigExtension> parseOperators(Collection<PhpClass> phpClasses) {
        Map<String, TwigExtension> extensions = new HashMap<String, TwigExtension>();
        for(PhpClass phpClass : phpClasses) {
            Method method = phpClass.findMethodByName("getOperators");
            if(method != null) {
                parseOperators(method, extensions);
            }
        }

        return extensions;
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
    protected Map<String, TwigExtension> parseOperators(Method method, Map<String, TwigExtension> filters) {

        final PhpClass containingClass = method.getContainingClass();
        if(containingClass == null) {
            return new HashMap<String, TwigExtension>();
        }

        /*
        return array(
            array(
                'not' => array(),
            ),
            array(
                'or' => array(),
            ),
        );
         */

        // getOperator return values, should one one by default
        for (PhpReturn phpReturn : PsiTreeUtil.findChildrenOfType(method, PhpReturn.class)) {

            // return element needs to be an array
            PhpPsiElement firstPsiChild = phpReturn.getFirstPsiChild();
            if(firstPsiChild instanceof ArrayCreationExpression) {

                // twig core returns nested array with 2 items array creation elements
                List<PsiElement> arrayValues = PhpPsiUtil.getChildren(firstPsiChild, new PsiElementTypCondition());
                if(arrayValues != null && arrayValues.size() > 0) {
                    for (PsiElement psiElement : arrayValues) {

                        // double check for non crazy syntax
                        if(!(psiElement instanceof PhpPsiElement)) {
                            continue;
                        }

                        // finally get all array keys with operator string
                        PhpPsiElement arrayValue = ((PhpPsiElement) psiElement).getFirstPsiChild();
                        if(arrayValue instanceof ArrayCreationExpression) {
                            for (ArrayHashElement arrayHashElement : PsiTreeUtil.findChildrenOfType(arrayValue, ArrayHashElement.class)) {
                                PhpPsiElement key = arrayHashElement.getKey();
                                String stringValue = PhpElementsUtil.getStringValue(key);
                                if(stringValue != null && StringUtils.isNotBlank(stringValue)) {
                                    filters.put(stringValue, new TwigExtension(TwigExtensionType.OPERATOR));
                                }
                            }
                        }

                    }
                }
            }
        }

        return filters;

    }

    protected Map<String, TwigExtension> parseSimpleTest(Method method, Map<String, TwigExtension> filters) {


        final PhpClass containingClass = method.getContainingClass();
        if(containingClass == null) {
            return new HashMap<String, TwigExtension>();
        }

        method.acceptChildren(new TwigSimpleTestVisitor(filters));

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

        if(twigExtensionType == TwigExtensionType.OPERATOR) {
            return PhpIcons.VARIABLE;
        }

        return PhpIcons.WEB_ICON;
    }

    @Nullable
    public static PsiElement getExtensionTarget(@NotNull Project project, @NotNull TwigExtension twigExtension) {
        String signature = twigExtension.getSignature();
        if(signature == null) {
            return null;
        }

        Collection<? extends PhpNamedElement> elements = PhpIndex.getInstance(project).getBySignature(signature);
        if(elements.size() == 0) {
            return null;
        }

        return elements.iterator().next();
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
            if(element instanceof NewExpression) {
                this.visitNewExpression((NewExpression) element);
            }
            super.visitElement(element);
        }

        private void visitNewExpression(NewExpression element) {

            // new \Twig_SimpleFunction('url', array($this, 'getUrl'), array('is_safe_callback' => array($this, 'isUrlGenerationSafe'))),
            if(PhpElementsUtil.isNewExpressionPhpClassWithInstance(element, "Twig_SimpleFilter")) {
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
            if(PhpElementsUtil.isNewExpressionPhpClassWithInstance(element, "Twig_Filter_Function")) {
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
            if(PhpElementsUtil.isNewExpressionPhpClassWithInstance(element, "Twig_Filter_Method")) {
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
            if(element instanceof NewExpression) {
                this.visitNewExpression((NewExpression) element);
            }
            super.visitElement(element);
        }

        private void visitNewExpression(NewExpression element) {

            // new \Twig_SimpleFunction('url', array($this, 'getUrl'), array('is_safe_callback' => array($this, 'isUrlGenerationSafe'))),
            if(PhpElementsUtil.isNewExpressionPhpClassWithInstance(element, "Twig_SimpleFunction")) {
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
            if(PhpElementsUtil.isNewExpressionPhpClassWithInstance(element, "Twig_Function_Method")) {
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
            if(PhpElementsUtil.isNewExpressionPhpClassWithInstance(element, "Twig_Function_Node")) {
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
        private final Map<String, TwigExtension> filters;

        public TwigSimpleTestVisitor(Map<String, TwigExtension> filters) {
            this.filters = filters;
        }

        @Override
        public void visitElement(PsiElement element) {
            if(element instanceof NewExpression) {
                this.visitNewExpression((NewExpression) element);
            }
            super.visitElement(element);
        }

        private void visitNewExpression(NewExpression element) {

            // new Twig_SimpleTest('even', null, array('node_class' => 'Twig_Node_Expression_Test_Even')),
            if(PhpElementsUtil.isNewExpressionPhpClassWithInstance(element, "Twig_SimpleTest")) {
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

    private static class PsiElementTypCondition implements Condition<PsiElement> {
        @Override
        public boolean value(PsiElement psiElement) {
            return psiElement.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE;
        }
    }
}
