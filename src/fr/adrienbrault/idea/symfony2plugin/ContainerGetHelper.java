package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpInstruction;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpReturnInstruction;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class ContainerGetHelper {

    public static boolean isContainerGetCall(PsiElement e) {
        return isContainerGetCall(e, 1);
    }

    public static boolean isContainerGetCall(PsiElement e, int deepness) {
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
        String methodFQN = method.getFQN(); // Something like "\Symfony\Bundle\FrameworkBundle\Controller\Controller.get"
        if (null == methodFQN) {
            return false;
        }

        String expectedMethodFQN = "\\Symfony\\Component\\DependencyInjection\\ContainerInterface.get";
        if (methodFQN.equals(expectedMethodFQN)) {
            return true;
        }

        if (deepness > 3) {
            return false;
        }

        // Try to see if this method return expression is a method call to a ContainerInterface::get ... recursive!

        PhpInstruction[] instructions = method.getControlFlow().getInstructions();
        for (int i = 0; i < instructions.length; i++) {
            PhpInstruction instruction = instructions[i];

            if (instruction instanceof PhpReturnInstruction) {
                PhpReturnInstruction returnInstruction = (PhpReturnInstruction) instruction;

                PsiElement returnInstructionElement = returnInstruction.getArgument();
                if (null != returnInstructionElement &&
                    null != returnInstructionElement.getReference() &&
                    returnInstructionElement.getReference().resolve() != resolvedReference) { // Avoid stackoverflow with method calling itself
                    return isContainerGetCall(returnInstructionElement, deepness + 1);
                }
            }
        }

        return false;
    }

    public static String getServiceId(MethodReference e) {
        String serviceId = null;

        PsiElement[] parameters = e.getParameters();
        if (parameters.length > 0 && parameters[0] instanceof StringLiteralExpression) {
            serviceId = parameters[0].getText(); // quoted string
            serviceId = serviceId.substring(1, serviceId.length() - 1);
        }

        return serviceId;
    }

}
