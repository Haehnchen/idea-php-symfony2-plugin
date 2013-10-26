package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.PhpLangUtil;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.patterns.PhpPatterns;
import com.jetbrains.php.lang.psi.elements.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PhpElementsUtil {
    static public List<ResolveResult> getClassInterfaceResolveResult(Project project, String FQNClassOrInterfaceName) {

        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<PhpClass> phpClasses = phpIndex.getClassesByFQN(FQNClassOrInterfaceName);
        Collection<PhpClass> phpInterfaces = phpIndex.getInterfacesByFQN(FQNClassOrInterfaceName);

        List<ResolveResult> results = new ArrayList<ResolveResult>();
        for (PhpClass phpClass : phpClasses) {
            results.add(new PsiElementResolveResult(phpClass));
        }
        for (PhpClass phpInterface : phpInterfaces) {
            results.add(new PsiElementResolveResult(phpInterface));
        }

        return results;
    }

    static public ArrayList<String> getArrayCreationKeys(ArrayCreationExpression arrayCreationExpression) {
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
        for(Method method: phpClass.getMethods()) {
            if(method.getName().equals(methodName)) {
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
        Collection<PhpClass> classes = phpIndex.getInterfacesByFQN(className);
        return classes.isEmpty() ? null : classes.iterator().next();
    }

    @Nullable
    static public PhpClass getClassInterface(Project project, String className) {
        PhpIndex phpIndex = PhpIndex.getInstance(project);
        PhpClass phpClass = getClass(phpIndex, className);
        if(phpClass != null) {
            return  phpClass;
        }

        phpClass = getInterface(phpIndex, className);
        if(phpClass != null) {
            return  phpClass;
        }

        return null;
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

    public static String getStringValue(PsiElement psiElement) {
        return getStringValue(psiElement, 0);
    }

    @Nullable
    private static String getStringValue(PsiElement psiElement, int depth) {

        if(++depth > 5) {
            return null;
        }

        if(psiElement instanceof StringLiteralExpression) {
            return ((StringLiteralExpression) psiElement).getContents();
        }

        if(psiElement instanceof PhpReference) {
            PsiElement ref = psiElement.getReference().resolve();

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

}
