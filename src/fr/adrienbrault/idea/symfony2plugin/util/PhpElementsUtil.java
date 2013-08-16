package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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

    static public void addClassPublicMethodCompletion(CompletionResultSet completionResultSet, PhpClass phpClass) {
        for(Method method: phpClass.getMethods()) {
            if(method.getAccess().isPublic() && !method.getName().startsWith("__")) {
                completionResultSet.addElement(new PhpLookupElement(method));
            }
        }
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

}
