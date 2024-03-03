package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.*;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtension;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigExtensionParser  {

    private static final Key<CachedValue<Map<String, TwigExtension>>> FUNCTION_CACHE = new Key<>("TWIG_EXTENSIONS_FUNCTION");
    private static final Key<CachedValue<Map<String, TwigExtension>>> TEST_CACHE = new Key<>("TWIG_EXTENSIONS_TEST");
    private static final Key<CachedValue<Map<String, TwigExtension>>> FILTERS_CACHE = new Key<>("TWIG_EXTENSIONS_FILTERS");
    private static final Key<CachedValue<Map<String, TwigExtension>>> OPERATORS_CACHE = new Key<>("TWIG_EXTENSIONS_OPERATORS");

    public enum TwigExtensionType {
        FUNCTION_METHOD, FUNCTION_NODE, SIMPLE_FUNCTION, FILTER, SIMPLE_TEST, OPERATOR
    }

    @NotNull
    public static Map<String, TwigExtension> getFunctions(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            FUNCTION_CACHE,
            () -> CachedValueProvider.Result.create(parseFunctions(TwigUtil.getTwigExtensionClasses(project)), PsiModificationTracker.MODIFICATION_COUNT),
            false
        );
    }

    @NotNull
    public static Map<String, TwigExtension> getFilters(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            FILTERS_CACHE,
            () -> CachedValueProvider.Result.create(parseFilters(TwigUtil.getTwigExtensionClasses(project)), PsiModificationTracker.MODIFICATION_COUNT),
            false
        );
    }

    @NotNull
    public static Map<String, TwigExtension> getSimpleTest(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            TEST_CACHE,
            () -> CachedValueProvider.Result.create(parseTests(TwigUtil.getTwigExtensionClasses(project)), PsiModificationTracker.MODIFICATION_COUNT),
            false
        );
    }

    @NotNull
    public static Map<String, TwigExtension> getOperators(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            OPERATORS_CACHE,
            () -> CachedValueProvider.Result.create(parseOperators(TwigUtil.getTwigExtensionClasses(project)), PsiModificationTracker.MODIFICATION_COUNT),
            false
        );
    }

    @NotNull
    private static Map<String, TwigExtension> parseFilters(@NotNull Collection<PhpClass> phpClasses) {
        Map<String, TwigExtension> extensions = new HashMap<>();

        for (PhpClass phpClass : phpClasses) {
            Method method = phpClass.findMethodByName("getFilters");
            if (method != null) {
                final PhpClass containingClass = method.getContainingClass();
                if (containingClass == null) {
                    continue;
                }

                for (NewExpression newExpression : PhpElementsUtil.collectNewExpressionsInsideControlFlow(method)) {
                    TwigFilterVisitor.visitNewExpression(newExpression, method, extensions, containingClass);
                }
            }
        }

        return Collections.unmodifiableMap(extensions);
    }

    @NotNull
    private static Map<String, TwigExtension> parseFunctions(@NotNull Collection<PhpClass> phpClasses) {
        Map<String, TwigExtension> extensions = new HashMap<>();

        for (PhpClass phpClass : phpClasses) {
            Method method = phpClass.findMethodByName("getFunctions");
            if (method != null) {
                final PhpClass containingClass = method.getContainingClass();
                if (containingClass == null) {
                    continue;
                }

                for (NewExpression newExpression : PhpElementsUtil.collectNewExpressionsInsideControlFlow(method)) {
                    TwigFunctionVisitor.visitNewExpression(newExpression, method, extensions, containingClass);
                }
            }
        }

        return Collections.unmodifiableMap(extensions);
    }

    @NotNull
    private static Map<String, TwigExtension> parseTests(@NotNull Collection<PhpClass> phpClasses) {
        Map<String, TwigExtension> extensions = new HashMap<>();

        for (PhpClass phpClass : phpClasses) {
            Method method = phpClass.findMethodByName("getTests");
            if (method != null) {
                for (NewExpression newExpression : PhpElementsUtil.collectNewExpressionsInsideControlFlow(method)) {
                    TwigSimpleTestVisitor.visitNewExpression(newExpression, extensions);
                }
            }
        }

        return Collections.unmodifiableMap(extensions);
    }

    @NotNull
    private static Map<String, TwigExtension> parseOperators(@NotNull Collection<PhpClass> phpClasses) {
        Map<String, TwigExtension> extensions = new HashMap<>();

        for (PhpClass phpClass : phpClasses) {
            Method method = phpClass.findMethodByName("getOperators");
            if (method != null) {
                parseOperators(method, extensions);
            }
        }

        return Collections.unmodifiableMap(extensions);
    }

    /**
     *  Get signature for callable like array($this, 'getUrl'), or 'function'
     */
    @Nullable
    private static String getCallableSignature(PsiElement psiElement, Method method) {
        // @TODO can be replaced by PhpStorm function now; much smarter
        // array($this, 'getUrl')
        if (psiElement instanceof ArrayCreationExpression) {
            List<PsiElement> arrayValues = (List<PsiElement>) PsiElementUtils.getChildrenOfTypeAsList(psiElement, PlatformPatterns.psiElement(PhpElementTypes.ARRAY_VALUE));
            if (arrayValues.size() > 1) {
                PsiElement firstChild = arrayValues.get(0).getFirstChild();
                if (firstChild instanceof Variable && "this".equals(((Variable) firstChild).getName())) {
                    String methodName = PhpElementsUtil.getStringValue(arrayValues.get(1).getFirstChild());
                    if (StringUtils.isNotBlank(methodName)) {
                        PhpClass phpClass = method.getContainingClass();
                        if (phpClass != null) {
                            return String.format("#M#C\\%s.%s", phpClass.getPresentableFQN(), methodName);
                        }
                    }
                } else if (firstChild instanceof ClassConstantReference) {
                    String classConstantPhpFqn = PhpElementsUtil.getClassConstantPhpFqn((ClassConstantReference) firstChild);
                    if (StringUtils.isNotEmpty(classConstantPhpFqn)) {
                        String methodName = PhpElementsUtil.getStringValue(arrayValues.get(1).getFirstChild());
                        if (StringUtils.isNotBlank(methodName)) {
                            PhpClass phpClass = method.getContainingClass();
                            if (phpClass != null) {
                                return String.format("#M#C\\%s.%s", classConstantPhpFqn, methodName);
                            }
                        }
                    }
                }
            }
        } else if (psiElement instanceof PhpCallableMethod) {
            // we need to resolve the type, no api support
            // $this->foobar(...)
            PsiElement firstChild = ((PhpCallableMethod) psiElement).getFirstPsiChild();
            if (firstChild instanceof Variable && "this".equals(((Variable) firstChild).getName())) {
                PhpClass phpClass = method.getContainingClass();
                if (phpClass != null) {
                    return String.format("#M#C\\%s.%s", phpClass.getPresentableFQN(), ((PhpCallableMethod) psiElement).getName());
                }
            }
        } else if (psiElement instanceof PhpCallableFunction) {
            // foobar(...)
            String name = ((PhpCallableFunction) psiElement).getName();
            if (StringUtils.isNotBlank(name)) {
                return "#F" + name;
            }
        } else {
            String funcTargetName = PhpElementsUtil.getStringValue(psiElement);
            if (funcTargetName != null) {

                if (funcTargetName.contains("::")) {
                    // 'SqlFormatter::format'
                    String[] splits = funcTargetName.split("::");
                    if (splits.length >= 2) {
                        return String.format("#M#C\\%s.%s", splits[0], splits[1]);
                    }
                } else {
                    return "#F" + funcTargetName;
                }
            }
        }

        return null;
    }

    private static void parseOperators(@NotNull Method method, @NotNull Map<String, TwigExtension> filters) {
        final PhpClass containingClass = method.getContainingClass();
        if (containingClass == null) {
            return;
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

        // getOperator return values, should one by default
        for (PsiElement phpReturnArgument : PhpElementsUtil.collectPhpReturnArgumentsInsideControlFlow(method)) {
            // return element needs to be an array
            if (phpReturnArgument instanceof ArrayCreationExpression arrayCreationExpression) {
                // twig core returns nested array with 2 items array creation elements
                List<PsiElement> arrayValues = PhpPsiUtil.getChildren(
                    arrayCreationExpression,
                    psiElement -> psiElement.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE
                );

                if (!arrayValues.isEmpty()) {
                    for (PsiElement psiElement : arrayValues) {

                        // double check for non-crazy syntax
                        if (!(psiElement instanceof PhpPsiElement)) {
                            continue;
                        }

                        // finally get all array keys with operator string
                        PhpPsiElement arrayValue = ((PhpPsiElement) psiElement).getFirstPsiChild();
                        if (arrayValue instanceof ArrayCreationExpression) {
                            for (ArrayHashElement arrayHashElement : PsiTreeUtil.findChildrenOfType(arrayValue, ArrayHashElement.class)) {
                                PhpPsiElement key = arrayHashElement.getKey();
                                String stringValue = PhpElementsUtil.getStringValue(key);
                                if (StringUtils.isNotBlank(stringValue)) {
                                    filters.put(stringValue, new TwigExtension(TwigExtensionType.OPERATOR));
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    @NotNull
    public static Icon getIcon(@NotNull TwigExtensionType twigExtensionType) {
        if (twigExtensionType == TwigExtensionType.FUNCTION_NODE) {
            return PhpIcons.CONSTRUCTOR;
        }

        if (twigExtensionType == TwigExtensionType.SIMPLE_FUNCTION) {
            return PhpIcons.FUNCTION;
        }

        if (twigExtensionType == TwigExtensionType.FUNCTION_METHOD) {
            return PhpIcons.METHOD;
        }

        if (twigExtensionType == TwigExtensionType.FILTER) {
            return PhpIcons.STATIC_FIELD;
        }

        if (twigExtensionType == TwigExtensionType.SIMPLE_TEST) {
            return PhpIcons.CONSTANT;
        }

        if (twigExtensionType == TwigExtensionType.OPERATOR) {
            return PhpIcons.VARIABLE;
        }

        return PhpIcons.CLASS_ATTRIBUTE;
    }

    @Nullable
    public static PsiElement getExtensionTarget(@NotNull Project project, @NotNull TwigExtension twigExtension) {
        String signature = twigExtension.getSignature();
        if (signature == null) {
            return null;
        }

        Collection<? extends PhpNamedElement> elements = PhpIndex.getInstance(project).getBySignature(signature);
        if (elements.isEmpty()) {
            return null;
        }

        return elements.iterator().next();
    }

    private static class TwigFilterVisitor {
        private static void visitNewExpression(@NotNull NewExpression element, @NotNull Method method, @NotNull Map<String, TwigExtension> filters, @NotNull PhpClass containingClass) {
            // new \Twig_SimpleFilter('url', array($this, 'getUrl'), array('is_safe_callback' => array($this, 'isUrlGenerationSafe'))),
            if (PhpElementsUtil.isNewExpressionPhpClassWithInstance(element, "Twig_SimpleFilter", "Twig_Filter", "Twig\\TwigFilter")) {
                PsiElement[] psiElement = element.getParameters();
                if (psiElement.length > 0) {
                    String funcName = PhpElementsUtil.getStringValue(psiElement[0]);
                    if (funcName != null && !funcName.contains("*")) {

                        String signature = null;
                        if (psiElement.length > 1) {
                            signature = getCallableSignature(psiElement[1], method);
                        }

                        // creation options like: needs_environment
                        Map<String, String> options;
                        if (psiElement.length > 2 && psiElement[2] instanceof ArrayCreationExpression) {
                            options = getOptions((ArrayCreationExpression) psiElement[2]);
                        } else {
                            options = new HashMap<>();
                        }

                        filters.put(funcName, new TwigExtension(TwigExtensionType.FILTER, signature, options));
                    }
                }

                return;
            }

            // array('shuffle' => new Twig_Filter_Function('twig_shuffle_filter'),)
            if (PhpElementsUtil.isNewExpressionPhpClassWithInstance(element, "Twig_Filter_Function")) {
                PsiElement arrayValue = element.getParent();
                if (arrayValue != null && arrayValue.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE) {
                    PsiElement arrayHash = arrayValue.getParent();
                    if (arrayHash instanceof ArrayHashElement) {
                        PsiElement arrayKey = ((ArrayHashElement) arrayHash).getKey();
                        String funcName = PhpElementsUtil.getStringValue(arrayKey);
                        if (funcName != null && !funcName.contains("*")) {

                            PsiElement[] parameters = element.getParameters();
                            String signature = null;
                            if (parameters.length > 0) {
                                signature = getCallableSignature(parameters[0], method);
                            }

                            filters.put(funcName, new TwigExtension(TwigExtensionType.FILTER, signature));
                        }
                    }
                }

                return;
            }

            // return array('serialize'  => new \Twig_Filter_Method($this, 'serialize'), );
            if (PhpElementsUtil.isNewExpressionPhpClassWithInstance(element, "Twig_Filter_Method")) {
                PsiElement arrayValue = element.getParent();
                if (arrayValue != null && arrayValue.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE) {
                    PsiElement arrayHash = arrayValue.getParent();
                    if (arrayHash instanceof ArrayHashElement) {
                        PsiElement arrayKey = ((ArrayHashElement) arrayHash).getKey();
                        String funcName = PhpElementsUtil.getStringValue(arrayKey);
                        if (funcName != null && funcName.matches("\\w+")) {

                            PsiElement[] parameters = element.getParameters();
                            String signature = null;
                            if (parameters.length > 1) {
                                if (parameters[0] instanceof Variable && "this".equals(((Variable) parameters[0]).getName())) {
                                    String methodName = PhpElementsUtil.getStringValue(parameters[1]);
                                    if (methodName != null) {
                                        signature = String.format("#M#C\\%s.%s", containingClass.getPresentableFQN(), methodName);
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
    static private Map<String, String> getOptions(@NotNull ArrayCreationExpression arrayCreationExpression) {
        Map<String, String> options = new HashMap<>();

        for (String optionTrue: new String[] {"needs_environment", "needs_context"}) {
            PhpPsiElement phpPsiElement = PhpElementsUtil.getArrayValue(arrayCreationExpression, optionTrue);
            if (phpPsiElement instanceof ConstantReference) {
                String value = phpPsiElement.getName();
                if (value != null && value.toLowerCase().equals("true")) {
                    options.put(optionTrue, "true");
                }
            }
        }

        return options;
    }

    private static class TwigFunctionVisitor {
        private static void visitNewExpression(@NotNull NewExpression element, @NotNull Method method, @NotNull Map<String, TwigExtension> filters, @NotNull PhpClass containingClass) {

            // new \Twig_SimpleFunction('url', array($this, 'getUrl'), array('is_safe_callback' => array($this, 'isUrlGenerationSafe'))),
            if (PhpElementsUtil.isNewExpressionPhpClassWithInstance(element, "Twig_SimpleFunction", "Twig_Function", "Twig\\TwigFunction")) {
                PsiElement[] psiElement = element.getParameters();
                if (psiElement.length > 0) {
                    String funcName = PhpElementsUtil.getStringValue(psiElement[0]);
                    if (funcName != null && !funcName.contains("*")) {

                        String signature = null;
                        if (psiElement.length > 1) {
                            signature = getCallableSignature(psiElement[1], method);
                        }

                        // creation options like: needs_environment
                        Map<String, String> options = new HashMap<>();
                        if (psiElement.length > 2 && psiElement[2] instanceof ArrayCreationExpression) {
                            options = getOptions((ArrayCreationExpression) psiElement[2]);
                        } else {
                            options = new HashMap<>();
                        }

                        filters.put(funcName, new TwigExtension(TwigExtensionType.SIMPLE_FUNCTION, signature, options));
                    }
                }

                return;
            }

            //array('form_javascript' => new \Twig_Function_Method($this, 'renderJavascript', array('is_safe' => array('html'))),);
            if (PhpElementsUtil.isNewExpressionPhpClassWithInstance(element, "Twig_Function_Method")) {
                PsiElement arrayValue = element.getParent();
                if (arrayValue != null && arrayValue.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE) {
                    PsiElement arrayHash = arrayValue.getParent();
                    if (arrayHash instanceof ArrayHashElement) {
                        PsiElement arrayKey = ((ArrayHashElement) arrayHash).getKey();
                        String funcName = PhpElementsUtil.getStringValue(arrayKey);
                        if (funcName != null && !funcName.contains("*")) {

                            PsiElement[] parameters = element.getParameters();
                            String signature = null;
                            if (parameters.length > 1) {
                                if (parameters[0] instanceof Variable && "this".equals(((Variable) parameters[0]).getName())) {
                                    String methodName = PhpElementsUtil.getStringValue(parameters[1]);
                                    if (methodName != null) {
                                        signature = String.format("#M#C\\%s.%s", containingClass.getPresentableFQN(), methodName);
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
            if (PhpElementsUtil.isNewExpressionPhpClassWithInstance(element, "Twig_Function_Node")) {
                PsiElement arrayValue = element.getParent();
                if (arrayValue != null && arrayValue.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE) {
                    PsiElement arrayHash = arrayValue.getParent();
                    if (arrayHash instanceof ArrayHashElement) {
                        PsiElement arrayKey = ((ArrayHashElement) arrayHash).getKey();
                        String funcName = PhpElementsUtil.getStringValue(arrayKey);
                        if (funcName != null && !funcName.contains("*")) {

                            PsiElement[] parameters = element.getParameters();
                            String signature = null;
                            if (parameters.length > 0) {
                                String className = PhpElementsUtil.getStringValue(parameters[0]);
                                if (className != null) {
                                    signature = String.format("#M#C\\%s.%s", StringUtils.stripStart(className, "\\"), "compile");
                                }
                            }

                            filters.put(funcName, new TwigExtension(TwigExtensionType.FUNCTION_NODE, signature));
                        }
                    }
                }
            }
        }
    }

    private static class TwigSimpleTestVisitor {
        private static void visitNewExpression(@NotNull NewExpression element, @NotNull Map<String, TwigExtension> filters) {
            // new Twig_SimpleTest('even', null, array('node_class' => 'Twig_Node_Expression_Test_Even')),
            if (PhpElementsUtil.isNewExpressionPhpClassWithInstance(element, "Twig_SimpleTest", "Twig_Test", "Twig\\TwigTest")) {
                PsiElement[] psiElement = element.getParameters();
                if (psiElement.length > 0) {
                    String funcName = PhpElementsUtil.getStringValue(psiElement[0]);
                    if (funcName != null && !funcName.contains("*")) {
                        String signature = null;

                        // new \Twig_SimpleTest('my_test', null, array('node_class' => 'My_Node_Test'))
                        if (psiElement.length > 1 && psiElement[1] instanceof StringLiteralExpression) {
                            String contents = ((StringLiteralExpression) psiElement[1]).getContents();
                            if (StringUtils.isNotBlank(contents)) {
                                signature = "#F" + contents;
                            }
                        }

                        // new \Twig_SimpleTest('empty', 'foo_test')
                        if (signature == null && psiElement.length > 2 && psiElement[2] instanceof ArrayCreationExpression) {
                            String nodeClass = PhpElementsUtil.getArrayHashValue((ArrayCreationExpression) psiElement[2], "node_class");
                            if (StringUtils.isNotBlank(nodeClass)) {
                                signature = String.format("#M#C\\%s.%s", StringUtils.stripStart(nodeClass, "\\"), "compile");
                            }
                        }

                        filters.put(funcName, new TwigExtension(TwigExtensionType.SIMPLE_TEST, signature));
                    }
                }
            }
        }
    }
}
