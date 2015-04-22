package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.PhpLangUtil;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.patterns.PhpPatterns;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.MethodReferenceBag;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PhpElementsUtil {
    static public List<ResolveResult> getClassInterfaceResolveResult(Project project, String fqnClassOrInterfaceName) {

        // api workaround for at least interfaces
        if(!fqnClassOrInterfaceName.startsWith("\\")) {
            fqnClassOrInterfaceName = "\\" + fqnClassOrInterfaceName;
        }

        List<ResolveResult> results = new ArrayList<ResolveResult>();
        for (PhpClass phpClass : PhpIndex.getInstance(project).getAnyByFQN(fqnClassOrInterfaceName)) {
            results.add(new PsiElementResolveResult(phpClass));
        }

        return results;
    }

    static public List<String> getArrayCreationKeys(ArrayCreationExpression arrayCreationExpression) {
        ArrayList<String> keys = new ArrayList<String>();

        for(ArrayHashElement arrayHashElement: arrayCreationExpression.getHashElements()) {
            PhpPsiElement child = arrayHashElement.getKey();
            if(child instanceof StringLiteralExpression) {
                keys.add(((StringLiteralExpression) child).getContents());
            }
        }

        return keys;
    }

    /**
     * array('foo' => 'bar', 'foo1' => 'bar')
     */
    static public HashMap<String, String> getArrayKeyValueMap(ArrayCreationExpression arrayCreationExpression) {
        HashMap<String, String> keys = new HashMap<String, String>();

        for(ArrayHashElement arrayHashElement: arrayCreationExpression.getHashElements()) {
            PhpPsiElement child = arrayHashElement.getKey();
            if(child != null && ((child instanceof StringLiteralExpression) || PhpPatterns.psiElement(PhpElementTypes.NUMBER).accepts(child))) {

                String key;
                if(child instanceof StringLiteralExpression) {
                    key = ((StringLiteralExpression) child).getContents();
                } else {
                    key = child.getText();
                }

                String value = null;

                if(arrayHashElement.getValue() instanceof StringLiteralExpression) {
                    value = ((StringLiteralExpression) arrayHashElement.getValue()).getContents();
                }

                keys.put(key, value);

            }
        }

        return keys;
    }

    @Nullable
    static public PhpPsiElement getArrayValue(ArrayCreationExpression arrayCreationExpression, String name) {

        for(ArrayHashElement arrayHashElement: arrayCreationExpression.getHashElements()) {
            PhpPsiElement child = arrayHashElement.getKey();
            if(child instanceof StringLiteralExpression) {
                if(((StringLiteralExpression) child).getContents().equals(name)) {
                    return arrayHashElement.getValue();
                }
            }
        }

        return null;
    }

    @Nullable
    static public String getArrayValueString(ArrayCreationExpression arrayCreationExpression, String name) {
        PhpPsiElement phpPsiElement = getArrayValue(arrayCreationExpression, name);
        if(phpPsiElement == null) {
            return null;
        }

        if(phpPsiElement instanceof StringLiteralExpression) {
            return ((StringLiteralExpression) phpPsiElement).getContents();
        }

        return null;
    }

    static public PsiElement[] getPsiElementsBySignature(Project project, @Nullable String signature) {

        if(signature == null) {
            return new PsiElement[0];
        }

        Collection<? extends PhpNamedElement> phpNamedElementCollections = PhpIndex.getInstance(project).getBySignature(signature, null, 0);
        return phpNamedElementCollections.toArray(new PsiElement[phpNamedElementCollections.size()]);
    }

    @Nullable
    static public PsiElement getPsiElementsBySignatureSingle(Project project, @Nullable String signature) {
        PsiElement[] psiElements = getPsiElementsBySignature(project, signature);
        if(psiElements.length == 0) {
            return null;
        }

        return psiElements[0];
    }

    @Deprecated
    static public PsiElement[] getClassInterfacePsiElements(Project project, String FQNClassOrInterfaceName) {

        // convert ResolveResult to PsiElement
        List<PsiElement> results = new ArrayList<PsiElement>();
        for(ResolveResult result: getClassInterfaceResolveResult(project, FQNClassOrInterfaceName)) {
            results.add(result.getElement());
        }

        return results.toArray(new PsiElement[results.size()]);
    }

    @Nullable
    static public Method getClassMethod(PhpClass phpClass, String methodName) {
        return phpClass.findMethodByName(methodName);
    }

    @Nullable
    static public Method getClassMethod(Project project, String phpClassName, String methodName) {

        // we need here an each; because eg Command is non unique because phar file
        for(PhpClass phpClass: PhpIndex.getInstance(project).getClassesByFQN(phpClassName)) {
            Method method = getClassMethod(phpClass, methodName);
            if(method != null) {
                return method;
            }
        }

        return null;
    }

    static public boolean isMethodWithFirstString(PsiElement psiElement, String... methodName) {

        // filter out method calls without parameter
        // $this->methodName('service_name')
        // withName is not working, so simulate it in a hack
        if(!PlatformPatterns
            .psiElement(PhpElementTypes.METHOD_REFERENCE)
            .withChild(PlatformPatterns
                .psiElement(PhpElementTypes.PARAMETER_LIST)
                .withFirstChild(PlatformPatterns
                    .psiElement(PhpElementTypes.STRING)
                )
            ).accepts(psiElement)) {

            return false;
        }

        // cant we move it up to PlatformPatterns? withName condition dont looks working
        String methodRefName = ((MethodReference) psiElement).getName();

        return null != methodRefName && Arrays.asList(methodName).contains(methodRefName);
    }

    /**
     * $this->methodName('service_name')
     * $this->methodName(SERVICE::NAME)
     * $this->methodName($this->name)
     */
    static public boolean isMethodWithFirstStringOrFieldReference(PsiElement psiElement, String... methodName) {

        if(!PlatformPatterns
            .psiElement(PhpElementTypes.METHOD_REFERENCE)
            .withChild(PlatformPatterns
                .psiElement(PhpElementTypes.PARAMETER_LIST)
                .withFirstChild(PlatformPatterns.or(
                    PlatformPatterns.psiElement(PhpElementTypes.STRING),
                    PlatformPatterns.psiElement(PhpElementTypes.FIELD_REFERENCE),
                    PlatformPatterns.psiElement(PhpElementTypes.CLASS_CONSTANT_REFERENCE)
                ))
            ).accepts(psiElement)) {

            return false;
        }

        // cant we move it up to PlatformPatterns? withName condition dont looks working
        String methodRefName = ((MethodReference) psiElement).getName();

        return null != methodRefName && Arrays.asList(methodName).contains(methodRefName);
    }

    static public PsiElementPattern.Capture<StringLiteralExpression> methodWithFirstStringPattern() {
        return PlatformPatterns
            .psiElement(StringLiteralExpression.class)
            .withParent(
                PlatformPatterns.psiElement(PhpElementTypes.PARAMETER_LIST)
                    .withFirstChild(
                        PlatformPatterns.psiElement(PhpElementTypes.STRING)
                    )
                    .withParent(
                        PlatformPatterns.psiElement(PhpElementTypes.METHOD_REFERENCE)
                    )
            )
            .withLanguage(PhpLanguage.INSTANCE);
    }

    /**
     * class "Foo" extends
     */
    static public PsiElementPattern.Capture<PsiElement> getClassNamePattern() {
        return PlatformPatterns
            .psiElement(PhpTokenTypes.IDENTIFIER)
            .afterLeafSkipping(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(PhpTokenTypes.kwCLASS)
            )
            .withParent(PhpClass.class)
            .withLanguage(PhpLanguage.INSTANCE);
    }

    /**
     * public function indexAction()
     */
    static public PsiElementPattern.Capture<PsiElement> getActionMethodPattern() {
        return PlatformPatterns
            .psiElement(PhpTokenTypes.IDENTIFIER).withText(
                PlatformPatterns.string().endsWith("Action")
            )
            .afterLeafSkipping(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(PhpTokenTypes.kwFUNCTION)
            )
            .inside(Method.class)
            .withLanguage(PhpLanguage.INSTANCE);
    }

    /**
     * return 'value' inside class method
     */
    static public PsiElementPattern.Capture<StringLiteralExpression> getMethodReturnPattern() {
        return PlatformPatterns
            .psiElement(StringLiteralExpression.class)
            .withParent(PlatformPatterns.psiElement(PhpReturn.class).inside(Method.class))
            .withLanguage(PhpLanguage.INSTANCE);
    }

    @Nullable
    static public PhpClass getClass(Project project, String className) {
        return getClass(PhpIndex.getInstance(project), className);
    }

    @Nullable
    static public PhpClass getClass(PhpIndex phpIndex, String className) {
        Collection<PhpClass> classes = phpIndex.getClassesByFQN(className);
        return classes.isEmpty() ? null : classes.iterator().next();
    }

    @Nullable
    static public PhpClass getInterface(PhpIndex phpIndex, String className) {

        // api workaround
        if(!className.startsWith("\\")) {
            className = "\\" + className;
        }

        Collection<PhpClass> classes = phpIndex.getInterfacesByFQN(className);
        return classes.isEmpty() ? null : classes.iterator().next();
    }

    @Nullable
    static public PhpClass getClassInterface(Project project, @NotNull String className) {

        // api workaround for at least interfaces
        if(!className.startsWith("\\")) {
            className = "\\" + className;
        }

        Collection<PhpClass> phpClasses = PhpIndex.getInstance(project).getAnyByFQN(className);
        return phpClasses.size() == 0 ? null : phpClasses.iterator().next();
    }

    static public Collection<PhpClass> getClassesInterface(Project project, @NotNull String className) {

        // api workaround for at least interfaces
        if(!className.startsWith("\\")) {
            className = "\\" + className;
        }

        return PhpIndex.getInstance(project).getAnyByFQN(className);
    }

    static public void addClassPublicMethodCompletion(CompletionResultSet completionResultSet, PhpClass phpClass) {
        for(Method method: getClassPublicMethod(phpClass)) {
            completionResultSet.addElement(new PhpLookupElement(method));
        }
    }

    static public ArrayList<Method> getClassPublicMethod(PhpClass phpClass) {
        ArrayList<Method> methods = new ArrayList<Method>();

        for(Method method: phpClass.getMethods()) {
            if(method.getAccess().isPublic() && !method.getName().startsWith("__")) {
                methods.add(method);
            }
        }

        return methods;
    }

    @Nullable
    static public String getArrayHashValue(ArrayCreationExpression arrayCreationExpression, String keyName) {
        ArrayHashElement translationArrayHashElement = PsiElementUtils.getChildrenOfType(arrayCreationExpression, PlatformPatterns.psiElement(ArrayHashElement.class)
            .withFirstChild(
                PlatformPatterns.psiElement(PhpElementTypes.ARRAY_KEY).withText(
                    PlatformPatterns.string().oneOf("'" + keyName + "'", "\"" + keyName + "\"")
                )
            )
        );

        if(translationArrayHashElement == null) {
            return null;
        }

        if(!(translationArrayHashElement.getValue() instanceof StringLiteralExpression)) {
            return null;
        }

        StringLiteralExpression valueString = (StringLiteralExpression) translationArrayHashElement.getValue();
        if(valueString == null) {
            return null;
        }

        return valueString.getContents();

    }

    static public boolean isEqualMethodReferenceName(MethodReference methodReference, String methodName) {
        String name = methodReference.getName();
        return name != null && name.equals(methodName);
    }

    static public PsiElement findArrayKeyValueInsideReference(PsiElement psiElement, String methodReferenceName, String keyName) {

        if(psiElement == null) {
            return null;
        }

        Collection<MethodReference> tests = PsiTreeUtil.findChildrenOfType(psiElement, MethodReference.class);
        for(MethodReference methodReference: tests) {

            // instance check
            // methodReference.getSignature().equals("#M#C\\Symfony\\Component\\OptionsResolver\\OptionsResolverInterface.setDefaults")
            if(PhpElementsUtil.isEqualMethodReferenceName(methodReference, methodReferenceName)) {
                PsiElement[] parameters = methodReference.getParameters();
                if(parameters.length > 0 && parameters[0] instanceof ArrayCreationExpression) {
                    PsiElement keyValue = PhpElementsUtil.getArrayValue((ArrayCreationExpression) parameters[0], keyName);
                    if(keyValue != null) {
                        return keyValue;
                    }
                }

            }

        }

        return null;
    }

    @Nullable
    static public String getArrayKeyValueInsideSignature(PsiElement psiElementInsideClass, String callTo, String methodName, String keyName) {
        PhpClass phpClass = PsiTreeUtil.getParentOfType(psiElementInsideClass, PhpClass.class);
        if(phpClass == null) {
            return null;
        }

        String className = phpClass.getPresentableFQN();
        if(className == null) {
            return null;
        }

        return PhpElementsUtil.getArrayKeyValueInsideSignature(psiElementInsideClass.getProject(), "#M#C\\" + className + "." + callTo, methodName, keyName);
    }

    @Nullable
    static public String getArrayKeyValueInsideSignature(Project project, String signature, String methodName, String keyName) {

        PsiElement psiElement = PhpElementsUtil.getPsiElementsBySignatureSingle(project, signature);
        if(psiElement == null) {
            return null;
        }

        Collection<MethodReference> tests = PsiTreeUtil.findChildrenOfType(psiElement, MethodReference.class);
        for(MethodReference methodReference: tests) {

            if(PhpElementsUtil.isEqualMethodReferenceName(methodReference, methodName)) {
                PsiElement[] parameters = methodReference.getParameters();
                if(parameters.length > 0 && parameters[0] instanceof ArrayCreationExpression) {
                    return PhpElementsUtil.getArrayValueString((ArrayCreationExpression) parameters[0], keyName);
                }

            }
        }

        return null;
    }


    public static Method[] getImplementedMethods(@NotNull Method method) {
        ArrayList<Method> items = getImplementedMethods(method.getContainingClass(), method, new ArrayList<Method>());
        return items.toArray(new Method[items.size()]);
    }


    private static ArrayList<Method> getImplementedMethods(@Nullable PhpClass phpClass, @NotNull Method method, ArrayList<Method> implementedMethods) {
        if (phpClass == null) {
            return implementedMethods;
        }

        Method[] methods = phpClass.getOwnMethods();
        for (Method ownMethod : methods) {
            if (PhpLangUtil.equalsMethodNames(ownMethod.getName(), method.getName())) {
                implementedMethods.add(ownMethod);
            }
        }

        for(PhpClass interfaceClass: phpClass.getImplementedInterfaces()) {
            getImplementedMethods(interfaceClass, method, implementedMethods);
        }

        getImplementedMethods(phpClass.getSuperClass(), method, implementedMethods);

        return implementedMethods;
    }

    @Nullable
    public static String getStringValue(@Nullable PsiElement psiElement) {
        return getStringValue(psiElement, 0);
    }

    @Nullable
    private static String getStringValue(@Nullable PsiElement psiElement, int depth) {

        if(psiElement == null || ++depth > 5) {
            return null;
        }

        if(psiElement instanceof StringLiteralExpression) {
            String resolvedString = ((StringLiteralExpression) psiElement).getContents();
            if(StringUtils.isEmpty(resolvedString)) {
                return null;
            }

            return resolvedString;
        }

        if(psiElement instanceof Field) {
            return getStringValue(((Field) psiElement).getDefaultValue(), depth);
        }

        if(psiElement instanceof PhpReference) {

            PsiReference psiReference = psiElement.getReference();
            if(psiReference == null) {
                return null;
            }

            PsiElement ref = psiReference.resolve();
            if(ref instanceof PhpReference) {
                return getStringValue(psiElement, depth);
            }

            if(ref instanceof Field) {
                PsiElement resolved = ((Field) ref).getDefaultValue();

                if(resolved instanceof StringLiteralExpression) {
                    return ((StringLiteralExpression) resolved).getContents();
                }
            }

        }

        return null;

    }

    public static String getPrevSiblingAsTextUntil(PsiElement psiElement, ElementPattern pattern, boolean includeMatching) {
        String prevText = "";

        for (PsiElement child = psiElement.getPrevSibling(); child != null; child = child.getPrevSibling()) {
            if(pattern.accepts(child)) {
                if(includeMatching) {
                    return child.getText() + prevText;
                }
                return prevText;
            } else {
                prevText = child.getText() + prevText;
            }
        }

        return prevText;
    }

    public static String getPrevSiblingAsTextUntil(PsiElement psiElement, ElementPattern pattern) {
        return getPrevSiblingAsTextUntil(psiElement, pattern, false);
    }

    @Nullable
    public static ArrayCreationExpression getCompletableArrayCreationElement(PsiElement psiElement) {

        // array('<test>' => '')
        if(PhpPatterns.psiElement(PhpElementTypes.ARRAY_KEY).accepts(psiElement.getContext())) {
            PsiElement arrayKey = psiElement.getContext();
            if(arrayKey != null) {
                PsiElement arrayHashElement = arrayKey.getContext();
                if(arrayHashElement instanceof ArrayHashElement) {
                    PsiElement arrayCreationExpression = arrayHashElement.getContext();
                    if(arrayCreationExpression instanceof ArrayCreationExpression) {
                        return (ArrayCreationExpression) arrayCreationExpression;
                    }
                }
            }

        }

        // on array creation key dont have value, so provide completion here also
        // array('foo' => 'bar', '<test>')
        if(PhpPatterns.psiElement(PhpElementTypes.ARRAY_VALUE).accepts(psiElement.getContext())) {
            PsiElement arrayKey = psiElement.getContext();
            if(arrayKey != null) {
                PsiElement arrayCreationExpression = arrayKey.getContext();
                if(arrayCreationExpression instanceof ArrayCreationExpression) {
                    return (ArrayCreationExpression) arrayCreationExpression;
                }

            }

        }

        return null;
    }

    public static Collection<PhpClass> getClassFromPhpTypeSet(Project project, Set<String> types) {

        PhpType phpType = new PhpType();
        phpType.add(types);

        List<PhpClass> phpClasses = new ArrayList<PhpClass>();

        for(String typeName: PhpIndex.getInstance(project).completeType(project, phpType, new HashSet<String>()).getTypes()) {
            if(typeName.startsWith("\\")) {
                PhpClass phpClass = PhpElementsUtil.getClassInterface(project, typeName);
                if(phpClass != null) {
                    phpClasses.add(phpClass);
                }
            }
        }

        return phpClasses;
    }

    public static Collection<PhpClass> getClassFromPhpTypeSetArrayClean(Project project, Set<String> types) {

        PhpType phpType = new PhpType();
        phpType.add(types);

        ArrayList<PhpClass> phpClasses = new ArrayList<PhpClass>();

        for(String typeName: PhpIndex.getInstance(project).completeType(project, phpType, new HashSet<String>()).getTypes()) {
            if(typeName.startsWith("\\")) {

                // we clean array types \Foo[]
                if(typeName.endsWith("[]")) {
                    typeName = typeName.substring(0, typeName.length() - 2);
                }

                PhpClass phpClass = PhpElementsUtil.getClassInterface(project, typeName);
                if(phpClass != null) {
                    phpClasses.add(phpClass);
                }
            }
        }

        return phpClasses;
    }

    @Nullable
    public static PhpClass getFirstClassFromFile(PhpFile phpFile) {
        Collection<PhpClass> phpClasses = PsiTreeUtil.collectElementsOfType(phpFile, PhpClass.class);
        return phpClasses.size() == 0 ? null : phpClasses.iterator().next();
    }

    public static boolean isEqualClassName(@Nullable PhpClass phpClass, @Nullable String... compareClassNames) {

        for(String className: compareClassNames) {
            if(isEqualClassName(phpClass, className)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isEqualClassName(@Nullable PhpClass phpClass, @Nullable String compareClassName) {

        if(phpClass == null || compareClassName == null) {
            return false;
        }

        String phpClassName = phpClass.getPresentableFQN();
        if(phpClassName == null) {
            return false;
        }

        if(phpClassName.startsWith("\\")) {
            phpClassName = phpClassName.substring(1);
        }

        if(compareClassName.startsWith("\\")) {
            compareClassName = compareClassName.substring(1);
        }

        return phpClassName.equals(compareClassName);
    }

    @Nullable
    public static PsiElement[] getMethodParameterReferences(Method method, int parameterIndex) {

        // we dont have a parameter on resolved method
        Parameter[] parameters = method.getParameters();
        if(parameters.length == 0 || parameterIndex >= parameters.length) {
            return null;
        }

        final String tempVariableName = parameters[parameterIndex].getName();
        return PsiTreeUtil.collectElements(method.getLastChild(), new PsiElementFilter() {
            @Override
            public boolean isAccepted(PsiElement element) {
                return element instanceof Variable && tempVariableName.equals(((Variable) element).getName());
            }
        });

    }


    @Nullable
    public static MethodReferenceBag getMethodParameterReferenceBag(PsiElement psiElement, int wantIndex) {

        PsiElement variableContext = psiElement.getContext();
        if(!(variableContext instanceof ParameterList)) {
            return null;
        }

        ParameterList parameterList = (ParameterList) variableContext;
        if (!(parameterList.getContext() instanceof MethodReference)) {
            return null;
        }

        ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(psiElement);
        if(currentIndex == null) {
            return null;
        }

        if(wantIndex >= 0 && currentIndex.getIndex() != wantIndex) {
            return null;
        }

        return new MethodReferenceBag(parameterList, (MethodReference) parameterList.getContext(), currentIndex);

    }

    @Nullable
    public static MethodReferenceBag getMethodParameterReferenceBag(PsiElement psiElement) {
        return getMethodParameterReferenceBag(psiElement, -1);
    }

    public static List<Variable> getVariableReferencesInScope(final Variable variable, final boolean includeSelf) {

        final List<Variable> variables = new ArrayList<Variable>();

        Variable variableDecl = null;
        if(!variable.isDeclaration()) {
            PsiElement psiElement = variable.resolve();
            if(psiElement instanceof Variable) {
                variableDecl = (Variable) psiElement;
            }
        } else {
            variableDecl = variable;
        }

        if(variableDecl == null) {
            return variables;
        }

        Method method = PsiTreeUtil.getParentOfType(variable, Method.class);

        PhpPsiUtil.hasReferencesInSearchScope(method.getUseScope(), variableDecl, new CommonProcessors.FindProcessor<PsiReference>() {
            @Override
            protected boolean accept(PsiReference psiReference) {

                PsiElement variableRef = psiReference.getElement();
                if (variableRef instanceof Variable) {
                    if(includeSelf) {
                        variables.add((Variable) variableRef);
                    } else {
                        if (!variableRef.equals(variable)) {
                            variables.add((Variable) variableRef);
                        }
                    }

                }

                return false;
            }
        });

        return variables;

    }

    /**
     * Try to visit possible class name for PsiElements with text like "Foo\|Bar", "Foo|\Bar", "\Foo|\Bar"
     * Cursor must have position in PsiElement
     *
     * @param psiElement the element context, cursor should be in it
     * @param cursorOffset current cursor editor eg from completion context
     * @param visitor callback on matching class
     */
    public static void visitNamespaceClassForCompletion(PsiElement psiElement, int cursorOffset, ClassForCompletionVisitor visitor) {

        int cursorOffsetClean = cursorOffset - psiElement.getTextOffset();
        if(cursorOffsetClean < 1) {
            return;
        }

        String content = psiElement.getText();
        int length = content.length();
        if(!(length >= cursorOffsetClean)) {
            return;
        }

        String beforeCursor = content.substring(0, cursorOffsetClean);
        boolean isValid;

        // espend\|Container, espend\Cont|ainer <- fallback to last full namespace
        // espend|\Container <- only on known namespace "espend"
        String namespace = beforeCursor;

        // if no backslash or its equal in first position, fallback on namespace completion
        int lastSlash = beforeCursor.lastIndexOf("\\");
        if(lastSlash <= 0) {
            isValid = PhpIndexUtil.hasNamespace(psiElement.getProject(), beforeCursor);
        } else {
            isValid = true;
            namespace = beforeCursor.substring(0, lastSlash);
        }

        if(!isValid) {
            return;
        }

        // format namespaces and add prefix for fluent completion
        String prefix = "";
        if(namespace.startsWith("\\")) {
            prefix = "\\";
        } else {
            namespace = "\\" + namespace;
        }

        // search classes in current namespace and child namespaces
        for(PhpClass phpClass: PhpIndexUtil.getPhpClassInsideNamespace(psiElement.getProject(), namespace)) {
            String presentableFQN = phpClass.getPresentableFQN();
            if(presentableFQN != null && fr.adrienbrault.idea.symfony2plugin.util.StringUtils.startWithEqualClassname(presentableFQN, beforeCursor)) {
                visitor.visit(phpClass, presentableFQN, prefix);
            }

        }

    }

    public static interface ClassForCompletionVisitor {
        public void visit(PhpClass phpClass, String presentableFQN, String prefix);
    }

    /**
     * new FooClass()
     */
    @Nullable
    private static PhpClass getNewExpressionPhpClass(@NotNull NewExpression newExpression) {
        ClassReference classReference = newExpression.getClassReference();
        if(classReference != null) {
            String fqn = classReference.getFQN();
            if(fqn != null) {
                return PhpElementsUtil.getClass(newExpression.getProject(), fqn);
            }
        }

        return null;
    }

    /**
     * Get PhpClass from "new FooClass()" only if match instance condition
     */
    public static PhpClass getNewExpressionPhpClassWithInstance(@NotNull NewExpression newExpression, @NotNull String instance) {

        PhpClass phpClass = getNewExpressionPhpClass(newExpression);
        if(phpClass != null && new Symfony2InterfacesUtil().isInstanceOf(phpClass, instance)) {
            return phpClass;
        }

        return null;
    }

    public static boolean isNewExpressionPhpClassWithInstance(@NotNull NewExpression newExpression, @NotNull String instance) {
        return getNewExpressionPhpClassWithInstance(newExpression, instance) != null;
    }


    @NotNull
    public static Collection<PsiElement> collectMethodElementsWithParents(final @NotNull Method method, @NotNull final Processor<PsiElement> processor) {
        Collection<PsiElement> elements = new HashSet<PsiElement>();
        collectMethodElementsWithParents(method, 3, elements, processor);
        return elements;
    }

    private static void collectMethodElementsWithParents(final @NotNull Method method, final int depth, @NotNull final Collection<PsiElement> elements, @NotNull final Processor<PsiElement> processor) {

        method.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement psiElement) {

                if(processor.process(psiElement)) {
                    elements.add(psiElement);
                }

                if(psiElement instanceof MethodReference && ((MethodReference) psiElement).getReferenceType() == PhpModifier.State.PARENT && method.getName().equals(((MethodReference) psiElement).getName())) {
                    PsiElement resolve = ((MethodReference) psiElement).resolve();
                    if(depth > 0 && resolve instanceof Method) {
                        collectMethodElementsWithParents((Method) resolve, depth - 1, elements, processor);
                    }
                }

                super.visitElement(psiElement);
            }
        });

    }

    /**
     * Gets parameter which are non optional and at the end of a function signature
     *
     * foo($container, $bar = null, $foo = null);
     *
     */
    @NotNull
    public static Parameter[] getFunctionRequiredParameter(@NotNull Function function) {

        // nothing we need to do
        Parameter[] parameters = function.getParameters();
        if(parameters.length == 0) {
            return new Parameter[0];
        }

        // find last optional parameter
        int last = -1;
        for (int i = parameters.length - 1; i >= 0; i--) {
            if(!parameters[i].isOptional()) {
                last = i;
                break;
            }
        }

        // no required argument found
        if(last == -1) {
            return new Parameter[0];
        }

        return Arrays.copyOfRange(parameters, 0, last + 1);
    }

}
