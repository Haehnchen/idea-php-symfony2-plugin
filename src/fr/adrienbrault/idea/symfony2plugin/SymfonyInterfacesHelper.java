package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpInstruction;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpReturnInstruction;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;

import java.util.Arrays;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class SymfonyInterfacesHelper {

    public static boolean isContainerGetCall(PsiElement e) {
        return isCallTo(e, getInterfaceMethod(e.getProject(), "\\Symfony\\Component\\DependencyInjection\\ContainerInterface", "get"));
    }

    public static boolean isTemplatingRenderCall(PsiElement e) {
        return isCallTo(e, new Method[] {
            getInterfaceMethod(e.getProject(), "\\Symfony\\Component\\Templating\\EngineInterface", "render"),
            getInterfaceMethod(e.getProject(), "\\Symfony\\Bundle\\TwigBundle\\EngineInterface", "renderResponse"),
        });
    }

    public static boolean isUrlGeneratorGenerateCall(PsiElement e) {
        return isCallTo(e, getInterfaceMethod(e.getProject(), "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface", "generate"));
    }

    private static boolean isCallTo(PsiElement e, Method expectedMethod) {
        return isCallTo(e, new Method[] { expectedMethod }, 1);
    }

    private static boolean isCallTo(PsiElement e, Method[] expectedMethods) {
        return isCallTo(e, expectedMethods, 1);
    }

    private static boolean isCallTo(PsiElement e, Method[] expectedMethods, int deepness) {
        if (!(e instanceof MethodReference)) {
            return false;
        }

        MethodReference methodRef = (MethodReference) e;
        if (null == e.getReference()) {
            return false;
        }

        PsiElement resolvedReference = methodRef.getReference().resolve();
        if (!(resolvedReference instanceof Method)) {
            return false;
        }

        Method method = (Method) resolvedReference;
        PhpClass methodClass = method.getContainingClass();

        for (Method expectedMethod : Arrays.asList(expectedMethods)) {
            if (null != expectedMethod && isInstanceOf(methodClass, expectedMethod.getContainingClass())) {
                return true;
            }
        }

        if (deepness > 5) {
            return false;
        }

        PhpInstruction[] instructions = method.getControlFlow().getInstructions();
        for (int i = 0; i < instructions.length; i++) {
            PhpInstruction instruction = instructions[i];

            if (instruction instanceof PhpReturnInstruction) {
                PhpReturnInstruction returnInstruction = (PhpReturnInstruction) instruction;

                PsiElement returnInstructionElement = returnInstruction.getArgument();
                if (null != returnInstructionElement &&
                    null != returnInstructionElement.getReference() &&
                    returnInstructionElement.getReference().resolve() != resolvedReference) { // Avoid stackoverflow with method calling itself

                    return isCallTo(returnInstructionElement, expectedMethods, deepness + 1);
                }
            }
        }


        return false;
    }

    public static String getFirstArgumentStringValue(MethodReference e) {
        String stringValue = null;

        PsiElement[] parameters = e.getParameters();
        if (parameters.length > 0 && parameters[0] instanceof StringLiteralExpression) {
            StringLiteralExpression stringLiteralExpression = (StringLiteralExpression)parameters[0];
            stringValue = stringLiteralExpression.getText(); // quoted string
            stringValue = stringValue.substring(stringLiteralExpression.getValueRange().getStartOffset(), stringLiteralExpression.getValueRange().getEndOffset());
        }

        return stringValue;
    }

    private static Method getInterfaceMethod(Project project, String interfaceFQN, String methodName) {
        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Object[] interfaces = phpIndex.getInterfacesByFQN(interfaceFQN).toArray();

        if (interfaces.length < 1) {
            return null;
        }

        return findClassMethodByName((PhpClass)interfaces[0], methodName);
    }

    private static Method getClassMethod(Project project, String classFQN, String methodName) {
        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Object[] classes = phpIndex.getClassesByFQN(classFQN).toArray();

        if (classes.length < 1) {
            return null;
        }

        return findClassMethodByName((PhpClass)classes[0], methodName);
    }

    private static Method findClassMethodByName(PhpClass phpClass, String methodName) {
        for (Method method : phpClass.getMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }

        return null;
    }

    private static boolean isImplementationOfInterface(PhpClass phpClass, PhpClass phpInterface) {
        if (phpClass == phpInterface) {
            return true;
        }

        for (PhpClass implementedInterface : phpClass.getImplementedInterfaces()) {
            if (isImplementationOfInterface(implementedInterface, phpInterface)) {
                return true;
            }
        }

        if (null == phpClass.getSuperClass()) {
            return false;
        }

        return isImplementationOfInterface(phpClass.getSuperClass(), phpInterface);
    }

    private static boolean isInstanceOf(PhpClass subjectClass, PhpClass expectedClass) {
        if (subjectClass == expectedClass) {
            return true;
        }

        if (expectedClass.isInterface()) {
            return isImplementationOfInterface(subjectClass, expectedClass);
        }

        if (null == subjectClass.getSuperClass()) {
            return false;
        }

        return isInstanceOf(subjectClass.getSuperClass(), expectedClass);
    }

}
