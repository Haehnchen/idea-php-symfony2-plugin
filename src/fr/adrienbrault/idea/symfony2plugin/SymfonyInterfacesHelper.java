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
public class SymfonyInterfacesHelper {

    public static boolean isContainerGetCall(PsiElement e) {
        return isCallTo(e, "\\Symfony\\Component\\DependencyInjection\\ContainerInterface.get");
    }

    public static boolean isTemplatingRenderCall(PsiElement e) {
        return isCallTo(e, new String[] {
            "\\Symfony\\Component\\Templating\\EngineInterface.render",
            "\\Symfony\\Bridge\\Twig\\TwigEngine.render",
            "\\Symfony\\Bundle\\TwigBundle\\TwigEngine.render",
            "\\Symfony\\Bundle\\TwigBundle\\TwigEngine.renderResponse",
            "\\Symfony\\Bundle\\TwigBundle\\EngineInterface.renderResponse"
        });
    }

    private static boolean isCallTo(PsiElement e, String expectedMethodFQN) {
        return isCallTo(e, new String[] { expectedMethodFQN }, 1);
    }

    private static boolean isCallTo(PsiElement e, String[] expectedMethodFQNs) {
        return isCallTo(e, expectedMethodFQNs, 1);
    }

    private static boolean isCallTo(PsiElement e, String[] expectedMethodFQNs, int deepness) {
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

        for (int i = 0; i < expectedMethodFQNs.length; i++) {
            String expectedMethodFQN = expectedMethodFQNs[i];

            if (methodFQN.equals(expectedMethodFQN)) {
                return true;
            }
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

                    return isCallTo(returnInstructionElement, expectedMethodFQNs, deepness + 1);
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

}
