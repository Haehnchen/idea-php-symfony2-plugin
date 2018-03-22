package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.codeInsight.PhpCodeInsightUtil;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.PhpLangUtil;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.patterns.PhpPatterns;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.phpunit.PhpUnitUtil;
import com.jetbrains.php.refactoring.PhpAliasImporter;
import fr.adrienbrault.idea.symfony2plugin.dic.MethodReferenceBag;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PsiElementAssertUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpElementsUtil {
    /**
     * Gets all array keys as string of an ArrayCreationExpression
     *
     * ['foo' => $bar]
     */
    @NotNull
    static public Collection<String> getArrayCreationKeys(@NotNull ArrayCreationExpression arrayCreationExpression) {
        return getArrayCreationKeyMap(arrayCreationExpression).keySet();
    }

    /**
     * Gets array key-value as single PsiElement map
     *
     * ['foo' => $bar]
     */
    @NotNull
    static public Map<String, PsiElement> getArrayCreationKeyMap(@NotNull ArrayCreationExpression arrayCreationExpression) {
        Map<String, PsiElement> keys = new HashMap<>();

        for(ArrayHashElement arrayHashElement: arrayCreationExpression.getHashElements()) {
            PhpPsiElement child = arrayHashElement.getKey();
            if(child instanceof StringLiteralExpression) {
                keys.put(((StringLiteralExpression) child).getContents(), child);
            }
        }

        return keys;
    }

    /**
     * Gets string values of array
     *
     * ["value", "value2"]
     */
    @NotNull
    static public Set<String> getArrayValuesAsString(@NotNull ArrayCreationExpression arrayCreationExpression) {
        return getArrayValuesAsMap(arrayCreationExpression).keySet();
    }

    /**
     * Get array string values mapped with their PsiElements
     *
     * ["value", "value2"]
     */
    @NotNull
    static public Map<String, PsiElement> getArrayValuesAsMap(@NotNull ArrayCreationExpression arrayCreationExpression) {
        Collection<PsiElement> arrayValues = PhpPsiUtil.getChildren(arrayCreationExpression, psiElement ->
            psiElement.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE
        );

        Map<String, PsiElement> keys = new HashMap<>();
        for (PsiElement child : arrayValues) {
            String stringValue = PhpElementsUtil.getStringValue(child.getFirstChild());
            if(stringValue != null && StringUtils.isNotBlank(stringValue)) {
                keys.put(stringValue, child);
            }
        }

        return keys;
    }

    /**
     * array('foo' => 'bar', 'foo1' => 'bar', 1 => 'foo')
     */
    @NotNull
    static public HashMap<String, String> getArrayKeyValueMap(@NotNull ArrayCreationExpression arrayCreationExpression) {
        HashMap<String, String> keys = new HashMap<>();

        for(ArrayHashElement arrayHashElement: arrayCreationExpression.getHashElements()) {
            PhpPsiElement child = arrayHashElement.getKey();
            if(child != null && ((child instanceof StringLiteralExpression) || PhpPatterns.psiElement(PhpElementTypes.NUMBER).accepts(child))) {

                String key;
                if(child instanceof StringLiteralExpression) {
                    key = ((StringLiteralExpression) child).getContents();
                } else {
                    key = child.getText();
                }

                if(key == null || StringUtils.isBlank(key)) {
                    continue;
                }

                String value = null;

                if(arrayHashElement.getValue() instanceof StringLiteralExpression) {
                    value = ((StringLiteralExpression) arrayHashElement.getValue()).getContents();
                }

                if(value == null || StringUtils.isBlank(value)) {
                    continue;
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
    static public String getArrayValueString(@NotNull ArrayCreationExpression arrayCreationExpression, @NotNull String name) {
        PhpPsiElement phpPsiElement = getArrayValue(arrayCreationExpression, name);
        if(phpPsiElement == null) {
            return null;
        }

        if(phpPsiElement instanceof StringLiteralExpression) {
            return ((StringLiteralExpression) phpPsiElement).getContents();
        }

        return null;
    }

    static public PsiElement[] getPsiElementsBySignature(@NotNull Project project, @Nullable String signature) {

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

    @Nullable
    static public Method getClassMethod(@NotNull Project project, @NotNull String phpClassName, @NotNull String methodName) {

        // we need here an each; because eg Command is non unique because phar file
        for(PhpClass phpClass: PhpIndex.getInstance(project).getClassesByFQN(phpClassName)) {
            Method method = phpClass.findMethodByName(methodName);
            if(method != null) {
                return method;
            }
        }

        return null;
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

    static public PsiElementPattern.Capture<StringLiteralExpression> getMethodWithFirstStringPattern() {
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
     * $foo->bar('<caret>')
     */
    static public PsiElementPattern.Capture<PsiElement> getParameterInsideMethodReferencePattern() {
        return PlatformPatterns
            .psiElement()
            .withParent(
                PlatformPatterns.psiElement(StringLiteralExpression.class)
                    .withParent(
                        PlatformPatterns.psiElement(ParameterList.class)
                            .withParent(
                                PlatformPatterns.psiElement(MethodReference.class)
                            )
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
     * return 'value' inside class method
     */
    static public ElementPattern<PhpExpression> getMethodReturnPattern() {
        return PlatformPatterns.or(
            PlatformPatterns.psiElement(StringLiteralExpression.class)
                .withParent(PlatformPatterns.psiElement(PhpReturn.class).inside(Method.class))
                .withLanguage(PhpLanguage.INSTANCE),
            PlatformPatterns.psiElement(ClassConstantReference.class)
                .withParent(PlatformPatterns.psiElement(PhpReturn.class).inside(Method.class))
                .withLanguage(PhpLanguage.INSTANCE)
        );
    }

    /**
     * Find a string return value of a method context "function() { return 'foo'}"
     * First match wins
     */
    @Nullable
    static public String getMethodReturnAsString(@NotNull PhpClass phpClass, @NotNull String methodName) {
        final Collection<String> values = getMethodReturnAsStrings(phpClass, methodName);
        if(values.size() == 0) {
            return null;
        }

        // we support only first item
        return values.iterator().next();
    }

    /**
     * Find a string return value of a method context "function() { return 'foo'}"
     */
    @NotNull
    static public Collection<String> getMethodReturnAsStrings(@NotNull PhpClass phpClass, @NotNull String methodName) {

        Method method = phpClass.findMethodByName(methodName);
        if(method == null) {
            return Collections.emptyList();
        }

        final Set<String> values = new HashSet<>();
        method.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {

                if(PhpElementsUtil.getMethodReturnPattern().accepts(element)) {
                    String value = PhpElementsUtil.getStringValue(element);
                    if(value != null && StringUtils.isNotBlank(value)) {
                        values.add(value);
                    }
                }

                super.visitElement(element);
            }
        });

        return values;
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
        Collection<PhpClass> classes = phpIndex.getInterfacesByFQN(className);
        return classes.isEmpty() ? null : classes.iterator().next();
    }

    @Nullable
    static public PhpClass getClassInterface(Project project, @NotNull String className) {
        Collection<PhpClass> phpClasses = PhpIndex.getInstance(project).getAnyByFQN(className);
        return phpClasses.size() == 0 ? null : phpClasses.iterator().next();
    }

    /**
     * @param subjectClass eg DateTime
     * @param expectedClass eg DateTimeInterface
     */
    public static boolean isInstanceOf(@NotNull PhpClass subjectClass, @NotNull PhpClass expectedClass) {
        return new PhpType().add(expectedClass).isConvertibleFrom(new PhpType().add(subjectClass), PhpIndex.getInstance(subjectClass.getProject()));
    }

    /**
     * @param subjectClass eg DateTime
     * @param expectedClass eg DateTimeInterface
     */
    public static boolean isInstanceOf(@NotNull PhpClass subjectClass, @NotNull String expectedClass) {
        return new PhpType().add(expectedClass).isConvertibleFrom(new PhpType().add(subjectClass), PhpIndex.getInstance(subjectClass.getProject()));
    }

    /**
     * @param subjectClass eg DateTime
     * @param expectedClass eg DateTimeInterface
     */
    public static boolean isInstanceOf(@NotNull Project project, @NotNull String subjectClass, @NotNull String expectedClass) {
        return new PhpType().add(expectedClass).isConvertibleFrom(new PhpType().add(subjectClass), PhpIndex.getInstance(project));
    }

    static public Collection<PhpClass> getClassesInterface(Project project, @NotNull String className) {
        return PhpIndex.getInstance(project).getAnyByFQN(className);
    }

    static public void addClassPublicMethodCompletion(CompletionResultSet completionResultSet, PhpClass phpClass) {
        for(Method method: getClassPublicMethod(phpClass)) {
            completionResultSet.addElement(new PhpLookupElement(method));
        }
    }

    static public ArrayList<Method> getClassPublicMethod(PhpClass phpClass) {
        ArrayList<Method> methods = new ArrayList<>();

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

    @Nullable
    static public PsiElement getArrayKeyValueInsideSignaturePsi(PsiElement psiElementInsideClass, String callTo[], String methodName, String keyName) {
        PhpClass phpClass = PsiTreeUtil.getParentOfType(psiElementInsideClass, PhpClass.class);
        if(phpClass == null) {
            return null;
        }

        String className = phpClass.getPresentableFQN();
        for (String s : callTo) {
            // @TODO: replace signature
            PsiElement arrayKeyValueInsideSignature = PhpElementsUtil.getArrayKeyValueInsideSignaturePsi(psiElementInsideClass.getProject(), "#M#C\\" + className + "." + s, methodName, keyName);
            if(arrayKeyValueInsideSignature != null) {
                return arrayKeyValueInsideSignature;
            }
        }

        return null;
    }

    @Nullable
    static public String getArrayKeyValueInsideSignature(PsiElement psiElementInsideClass, String callTo[], String methodName, String keyName) {
        return getStringValue(getArrayKeyValueInsideSignaturePsi(psiElementInsideClass, callTo, methodName, keyName));
    }

    @Nullable
    static private PsiElement getArrayKeyValueInsideSignaturePsi(Project project, String signature, String methodName, String keyName) {
        PsiElement psiElement = PhpElementsUtil.getPsiElementsBySignatureSingle(project, signature);

        if(psiElement == null) {
            return null;
        }

        for(MethodReference methodReference: PsiTreeUtil.findChildrenOfType(psiElement, MethodReference.class)) {
            if(PhpElementsUtil.isEqualMethodReferenceName(methodReference, methodName)) {
                PsiElement[] parameters = methodReference.getParameters();
                if(parameters.length > 0 && parameters[0] instanceof ArrayCreationExpression) {
                    return PhpElementsUtil.getArrayValue((ArrayCreationExpression) parameters[0], keyName);
                }
            }
        }

        return null;
    }

    public static Method[] getImplementedMethods(@NotNull Method method) {
        ArrayList<Method> items = getImplementedMethods(method.getContainingClass(), method, new ArrayList<>());
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

    /**
     * Resolve string definition in a recursive way
     *
     * $foo = Foo::class
     * $this->foo = Foo::class
     * $this->foo1 = $this->foo
     */
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
        } else if(psiElement instanceof Field) {
            return getStringValue(((Field) psiElement).getDefaultValue(), depth);
        } else if(psiElement instanceof ClassConstantReference && "class".equals(((ClassConstantReference) psiElement).getName())) {
            // Foobar::class
            return getClassConstantPhpFqn((ClassConstantReference) psiElement);
        } else if(psiElement instanceof PhpReference) {
            PsiReference psiReference = psiElement.getReference();
            if(psiReference == null) {
                return null;
            }

            PsiElement ref = psiReference.resolve();
            if(ref instanceof PhpReference) {
                return getStringValue(psiElement, depth);
            }

            if(ref instanceof Field) {
                return getStringValue(((Field) ref).getDefaultValue());
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
        for (String type : types) {
            phpType.add(type);
        }

        List<PhpClass> phpClasses = new ArrayList<>();

        for(String typeName: PhpIndex.getInstance(project).completeType(project, phpType, new HashSet<>()).getTypes()) {
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
        for (String type : types) {
            phpType.add(type);
        }

        ArrayList<PhpClass> phpClasses = new ArrayList<>();

        for(String typeName: PhpIndex.getInstance(project).completeType(project, phpType, new HashSet<>()).getTypes()) {
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

    public static boolean isEqualClassName(@NotNull PhpClass phpClass, @NotNull PhpClass compareClassName) {
        return isEqualClassName(phpClass, compareClassName.getPresentableFQN());
    }

    public static boolean isEqualClassName(@Nullable PhpClass phpClass, @Nullable String compareClassName) {
        if(phpClass == null || compareClassName == null) {
            return false;
        }

        return StringUtils.stripStart(phpClass.getPresentableFQN(), "\\")
            .equals(StringUtils.stripStart(compareClassName, "\\"));
    }

    @NotNull
    public static PsiElement[] getMethodParameterReferences(@NotNull Method method, int parameterIndex) {
        // we dont have a parameter on resolved method
        Parameter[] parameters = method.getParameters();

        if(parameters.length == 0 || parameterIndex >= parameters.length) {
            return new PsiElement[0];
        }

        String tempVariableName = parameters[parameterIndex].getName();

        return PsiTreeUtil.collectElements(method.getLastChild(), element ->
            element instanceof Variable && tempVariableName.equals(((Variable) element).getName())
        );
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

    /**
     * Find all variables in current function / method scope
     *
     * $v<caret>ar = 'foobar';
     * $v<caret>ar->foo()
     */
    public static Collection<Variable> getVariableReferencesInScope(@NotNull Variable variable) {
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
            return Collections.emptyList();
        }

        Function function = PsiTreeUtil.getParentOfType(variable, Function.class);
        if(function == null) {
            return Collections.emptyList();
        }

        final List<Variable> variables = new ArrayList<>();

        for (Variable variableRef : PhpElementsUtil.getVariablesInScope(function, variableDecl)) {
            if (!variableRef.equals(variable)) {
                variables.add(variableRef);
            }
        }

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
            if(fr.adrienbrault.idea.symfony2plugin.util.StringUtils.startWithEqualClassname(presentableFQN, beforeCursor)) {
                visitor.visit(phpClass, presentableFQN, prefix);
            }

        }

    }

    public interface ClassForCompletionVisitor {
        void visit(PhpClass phpClass, String presentableFQN, String prefix);
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
        if(phpClass != null && PhpElementsUtil.isInstanceOf(phpClass, instance)) {
            return phpClass;
        }

        return null;
    }

    /**
     * Check new class is instance of given type eg: "new \Twig_Test()"
     */
    public static boolean isNewExpressionPhpClassWithInstance(@NotNull NewExpression newExpression, @NotNull String... instances) {
        for (String instance : instances) {
            PhpClass phpClass = getNewExpressionPhpClassWithInstance(newExpression, instance);
            if(phpClass != null) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    public static Collection<PsiElement> collectMethodElementsWithParents(final @NotNull Method method, @NotNull final Processor<PsiElement> processor) {
        Collection<PsiElement> elements = new HashSet<>();
        collectMethodElementsWithParents(method, 3, elements, processor);
        return elements;
    }

    private static void collectMethodElementsWithParents(final @NotNull Method method, final int depth, @NotNull final Collection<PsiElement> elements, @NotNull final Processor<PsiElement> processor) {

        method.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement psiElement) {

                if (processor.process(psiElement)) {
                    elements.add(psiElement);
                }

                if (psiElement instanceof MethodReference && ((MethodReference) psiElement).getReferenceType() == PhpModifier.State.PARENT && method.getName().equals(((MethodReference) psiElement).getName())) {
                    PsiElement resolve = ((MethodReference) psiElement).resolve();
                    if (depth > 0 && resolve instanceof Method) {
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

    public static boolean isTestClass(@NotNull PhpClass phpClass) {

        if(PhpUnitUtil.isTestClass(phpClass)) {
            return true;
        }

        String fqn = phpClass.getPresentableFQN();
        return fqn.contains("\\Test\\") || fqn.contains("\\Tests\\");
    }

    /**
     * Extract type hint from method parameter
     *
     * function foo(\FooClass $class)
     */
    @Nullable
    public static String getMethodParameterTypeHint(@NotNull Method method) {
        ParameterList childOfType = PsiTreeUtil.getChildOfType(method, ParameterList.class);
        if(childOfType == null) {
            return null;
        }

        PsiElement[] parameters = childOfType.getParameters();
        if(parameters.length == 0) {
            return null;
        }

        ClassReference classReference = PsiTreeUtil.getChildOfType(parameters[0], ClassReference.class);
        if(classReference == null) {
            return null;
        }

        return classReference.getFQN();
    }

    /**
     * Find first variable declaration in parent scope of a given variable:
     *
     * function() {
     *   $event = new FooEvent();
     *   dispatch('foo', $event);
     * }
     */
    @Nullable
    public static String getFirstVariableTypeInScope(@NotNull Variable variable) {

        // parent search scope, eg Method else fallback to a grouped statement
        PsiElement searchScope = PsiTreeUtil.getParentOfType(variable, Function.class);
        if(searchScope == null) {
            searchScope = PsiTreeUtil.getParentOfType(variable, GroupStatement.class);
        }

        if(searchScope == null) {
            return null;
        }

        final String name = variable.getName();
        if(name == null) {
            return null;
        }

        final String[] result = {null};
        searchScope.acceptChildren(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if(element instanceof Variable && name.equals(((Variable) element).getName())) {
                    PsiElement assignmentExpression = element.getParent();
                    if(assignmentExpression instanceof AssignmentExpression) {
                        PhpPsiElement value = ((AssignmentExpression) assignmentExpression).getValue();
                        if(value instanceof NewExpression) {
                            ClassReference classReference = ((NewExpression) value).getClassReference();
                            if(classReference != null) {
                                String classSignature = classReference.getFQN();
                                if(StringUtils.isNotBlank(classSignature)) {
                                    result[0] = classSignature;
                                }
                            }
                        }
                    }
                }

                super.visitElement(element);
            }
        });

        return result[0];
    }

    /**
     * Get class by shortcut namespace, on a scoped namespace

     * @param project current project
     * @param classNameScope Namespace fo search "\Foo\Foo", "Foo\Foo", "Foo\Foo\", last "\*" is stripped
     * @param className Class name inside namespace also fqn is supported

     * @return PhpClass matched
     */
    public static PhpClass getClassInsideNamespaceScope(@NotNull Project project, @NotNull String classNameScope, @NotNull String className) {

        if(className.startsWith("\\")) {
            return PhpElementsUtil.getClassInterface(project, className);
        }

        // strip class name we namespace
        String strip = StringUtils.strip(classNameScope, "\\");
        int i = strip.lastIndexOf("\\");
        if(i <= 0) {
            return PhpElementsUtil.getClassInterface(project, className);
        }

        PhpClass phpClass = PhpElementsUtil.getClassInterface(project, strip.substring(0, i) + "\\" + StringUtils.strip(className, "\\"));
        if(phpClass != null) {
            return phpClass;
        }

        return PhpElementsUtil.getClassInterface(project, className);
    }

    /**
     * Resolves MethodReference and compare containing class against implementations instances
     */
    public static boolean isMethodReferenceInstanceOf(@NotNull MethodReference methodReference, @NotNull String expectedClassName) {
        for (ResolveResult resolveResult : methodReference.multiResolve(false)) {
            PsiElement resolve = resolveResult.getElement();

            if(!(resolve instanceof Method)) {
                continue;
            }

            PhpClass containingClass = ((Method) resolve).getContainingClass();
            if(containingClass == null) {
                continue;
            }

            if(!PhpElementsUtil.isInstanceOf(containingClass, expectedClassName)) {
                continue;
            }

            return true;
        }

        return false;
    }

    /**
     * Resolves MethodReference and compare containing class against implementations instances
     */
    public static boolean isMethodReferenceInstanceOf(@NotNull MethodReference methodReference, @NotNull String expectedClassName, @NotNull String methodName) {
        if(!methodName.equals(methodReference.getName())) {
            return false;
        }

        return isMethodReferenceInstanceOf(methodReference, expectedClassName);
    }


    /**
     * Try to find method matching on any "className::method" giving
     */
    public static boolean isMethodReferenceInstanceOf(@NotNull MethodReference methodReference, @NotNull MethodMatcher.CallToSignature... signatures) {
        for (MethodMatcher.CallToSignature method : signatures) {
            if (isMethodReferenceInstanceOf(methodReference, method.getInstance(), method.getMethod())) {
                return true;
            }
        }

        return false;
    }

    public static boolean isMethodInstanceOf(@NotNull Method method, @NotNull String clazz, @NotNull String methodName) {
        return isMethodInstanceOf(method, new MethodMatcher.CallToSignature(clazz, methodName));
    }

    public static boolean isMethodInstanceOf(Method method, @NotNull MethodMatcher.CallToSignature... signatures) {
        PhpClass containingClass = method.getContainingClass();
        if(containingClass == null) {
            return false;
        }

        for (MethodMatcher.CallToSignature signature : signatures) {
            String methodName = signature.getMethod();

            if(!methodName.equals(method.getName())) {
                continue;
            }

            if (PhpElementsUtil.isInstanceOf(containingClass, signature.getInstance())) {
                return true;
            }
        }

        return false;
    }

    public static void replaceElementWithClassConstant(@NotNull PhpClass phpClass, @NotNull PsiElement originElement) throws Exception{
        String fqn = phpClass.getFQN();
        if(!fqn.startsWith("\\")) {
            fqn = "\\" + fqn;
        }

        PhpPsiElement scopeForUseOperator = PhpCodeInsightUtil.findScopeForUseOperator(originElement);
        if(scopeForUseOperator == null) {
            throw new Exception("Class fqn error");
        }

        if(!PhpCodeInsightUtil.getAliasesInScope(scopeForUseOperator).values().contains(fqn)) {
            PhpAliasImporter.insertUseStatement(fqn, scopeForUseOperator);
        }

        originElement.replace(PhpPsiElementFactory.createPhpPsiFromText(
            originElement.getProject(),
            ClassConstantReference.class,
            "<?php " + phpClass.getName() + "::class"
        ));
    }

    /**
     * add('', <caret>), add('', Foo<caret>)
     */
    @Nullable
    public static MethodReference findMethodReferenceOnClassConstant(PsiElement psiElement) {
        PsiElement parameterList = psiElement.getParent();
        if(parameterList instanceof ParameterList) {
            PsiElement psiElement2 = parameterList.getParent();
            if(psiElement2 instanceof MethodReference) {
                return (MethodReference) psiElement2;
            }
        } else if(parameterList instanceof ConstantReference) {
            PsiElement parent = parameterList.getParent();
            if(parent instanceof ParameterList) {
                return PsiElementAssertUtil.getParentOfTypeOrNull(parent, MethodReference.class);
            }
        }

        return null;
    }

    /**
     * Foo::class to its PhpClass
     */
    public static PhpClass getClassConstantPhpClass(@NotNull ClassConstantReference classConstant) {
        String typeName = getClassConstantPhpFqn(classConstant);
        return typeName != null ? PhpElementsUtil.getClassInterface(classConstant.getProject(), typeName) : null;
    }

    /**
     * Foo::class to its class fqn include namespace
     */
    public static String getClassConstantPhpFqn(@NotNull ClassConstantReference classConstant) {
        PhpExpression classReference = classConstant.getClassReference();
        if(!(classReference instanceof PhpReference)) {
            return null;
        }

        String typeName = ((PhpReference) classReference).getFQN();
        return typeName != null && StringUtils.isNotBlank(typeName) ? StringUtils.stripStart(typeName, "\\") : null;
    }

    /**
     * Get type hint PhpClass of an given method index
     *
     * @param parameterIndex staring 0
     */
    @Nullable
    public static PhpClass getMethodTypeHintParameterPhpClass(@NotNull Method method, int parameterIndex) {
        Parameter[] constructorParameter = method.getParameters();
        if(parameterIndex >= constructorParameter.length) {
            return null;
        }

        String className = constructorParameter[parameterIndex].getDeclaredType().toString();
        if(StringUtils.isBlank(className)) {
            return null;
        }

        return PhpElementsUtil.getClassInterface(method.getProject(), className);
    }

    @Nullable
    public static String insertUseIfNecessary(@NotNull PsiElement phpClass, @NotNull String fqnClasName) {
        if(!fqnClasName.startsWith("\\")) {
            fqnClasName = "\\" + fqnClasName;
        }

        PhpPsiElement scopeForUseOperator = PhpCodeInsightUtil.findScopeForUseOperator(phpClass);
        if(scopeForUseOperator == null) {
            return null;
        }

        if(!PhpCodeInsightUtil.getAliasesInScope(scopeForUseOperator).values().contains(fqnClasName)) {
            PhpAliasImporter.insertUseStatement(fqnClasName, scopeForUseOperator);
        }

        for (Map.Entry<String, String> entry : PhpCodeInsightUtil.getAliasesInScope(scopeForUseOperator).entrySet()) {
            if(fqnClasName.equals(entry.getValue())) {
                return entry.getKey();
            }
        }

        return null;
    }

    /**
     * Collects all variables in a given scope.
     * Eg find all variables usages in a given method
     */
    @NotNull
    public static Set<Variable> getVariablesInScope(@NotNull PsiElement psiElement, @NotNull PhpNamedElement variable) {
        return MyVariableRecursiveElementVisitor.visit(psiElement, variable.getName());
    }

    /**
     * Provide array key pattern. we need incomplete array key support, too.
     *
     * foo(['<caret>'])
     * foo(['<caret>' => 'foobar'])
     */
    @NotNull
    public static PsiElementPattern.Capture<PsiElement> getParameterListArrayValuePattern() {
        return PlatformPatterns.psiElement()
            .withParent(PlatformPatterns.psiElement(StringLiteralExpression.class).withParent(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement().withElementType(PhpElementTypes.ARRAY_VALUE)
                        .withParent(PlatformPatterns.psiElement(ArrayCreationExpression.class)
                            .withParent(ParameterList.class)
                        ),

                    PlatformPatterns.psiElement().withElementType(PhpElementTypes.ARRAY_KEY)
                        .withParent(PlatformPatterns.psiElement(ArrayHashElement.class)
                            .withParent(PlatformPatterns.psiElement(ArrayCreationExpression.class)
                                .withParent(ParameterList.class)
                            )
                        )
                ))
            );
    }

    /**
     * true ? "foo" : "Foobar::class"
     */
    @NotNull
    public static Collection<String> getTernaryExpressionConditionStrings(@NotNull TernaryExpression firstPsiChild) {
        Collection<String> types = new ArrayList<>();

        PsiElement[] psiElements = {
            firstPsiChild.getFalseVariant(),
            firstPsiChild.getTrueVariant()
        };

        for (PsiElement psiElement : psiElements) {
            if(psiElement != null) {
                String stringValue = PhpElementsUtil.getStringValue(psiElement);
                if(stringValue != null) {
                    types.add(stringValue);
                }
            }
        }

        return types;
    }

    /**
     * Find argument by name in constructor parameter: __construct($foobar)
     */
    public static int getConstructorArgumentByName(@NotNull PhpClass phpClass, @NotNull String argumentName) {
        Method constructor = phpClass.getConstructor();
        if(constructor == null) {
            return -1;
        }

        return getFunctionArgumentByName(constructor, argumentName);
    }

    /**
     * Find argument by name in function parameter: argumentName($foobar)
     */
    public static int getFunctionArgumentByName(@NotNull Function function, @NotNull String argumentName) {
        Parameter[] parameters = function.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (argumentName.equals(parameter.getName())) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Single resolve doesnt work if we have non unique class names in project context,
     * so try a multiResolve
     */
    @NotNull
    public static Method[] getMultiResolvedMethod(@NotNull PsiReference psiReference) {
        // class be unique in normal case, so try this first
        PsiElement resolvedReference = psiReference.resolve();
        if (resolvedReference instanceof Method) {
            return new Method[] { (Method) resolvedReference };
        }

        // try multiResolve if class exists twice in project
        if(psiReference instanceof PsiPolyVariantReference) {
            Collection<Method> methods = new HashSet<>();
            for(ResolveResult resolveResult : ((PsiPolyVariantReference) psiReference).multiResolve(false)) {
                PsiElement element = resolveResult.getElement();
                if(element instanceof Method) {
                    methods.add((Method) element);
                }
            }

            if(methods.size() > 0) {
                return methods.toArray(new Method[methods.size()]);
            }

        }

        return new Method[0];
    }

    /**
     * Get first string value of MethodReference; Not index access allowed!
     * see getMethodReferenceStringValueParameter for resolving value in detail
     */
    @Nullable
    public static String getFirstArgumentStringValue(@NotNull MethodReference methodReference) {
        String stringValue = null;

        PsiElement[] parameters = methodReference.getParameters();
        if (parameters.length > 0 && parameters[0] instanceof StringLiteralExpression) {
            stringValue = ((StringLiteralExpression) parameters[0]).getContents();
        }

        return stringValue;
    }

    /**
     * Get resolved string value
     *
     * $f->foo('index_0', 'index_1');
     */
    @Nullable
    public static String getMethodReferenceStringValueParameter(@NotNull MethodReference methodReference, int parameter) {
        PsiElement[] parameters = methodReference.getParameters();
        if (parameters.length > parameter) {
            return getStringValue(parameters[parameter]);
        }

        return null;
    }

    /**
     * Visit and collect all variables in given scope
     */
    private static class MyVariableRecursiveElementVisitor extends PsiRecursiveElementVisitor {
        @NotNull
        private final String name;

        @NotNull
        private final Set<Variable> variables = new HashSet<>();

        MyVariableRecursiveElementVisitor(@NotNull String name) {
            this.name = name;
        }

        @Override
        public void visitElement(PsiElement element) {
            if(element instanceof Variable && name.equals(((Variable) element).getName())) {
                variables.add((Variable) element);
            }
            super.visitElement(element);
        }

        public static Set<Variable> visit(@NotNull PsiElement scope, @NotNull String name) {
            MyVariableRecursiveElementVisitor visitor = new MyVariableRecursiveElementVisitor(name);
            scope.acceptChildren(visitor);
            return visitor.variables;
        }
    }
}
