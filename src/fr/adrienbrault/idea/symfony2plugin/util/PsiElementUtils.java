package fr.adrienbrault.idea.symfony2plugin.util;


import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PsiElementUtils {
    public static String getMethodParameter(PsiElement parameter) {

        if (!(parameter instanceof StringLiteralExpression)) {
            return null;
        }

        StringLiteralExpression stringLiteralExpression = (StringLiteralExpression) parameter;

        String stringValue = stringLiteralExpression.getText();
        String value = stringValue.substring(stringLiteralExpression.getValueRange().getStartOffset(), stringLiteralExpression.getValueRange().getEndOffset());

        // wtf: ???
        // looks like current cursor position is marked :)
        value = value.replace("IntellijIdeaRulezzz", "");
        value = value.replace("IntellijIdeaRulezzz ", "");

        return value;
    }

    public static String getMethodParameterAt(ParameterList parameterList, int index) {
        PsiElement[] parameters = parameterList.getParameters();

        if(parameters.length < index) {
            return null;
        }

        return getMethodParameter(parameters[index]);
    }

    @Nullable
    public static ParameterBag getCurrentParameterIndex(PsiElement[] parameters, PsiElement parameter) {
        int i;
        for(i = 0; i < parameters.length; i = i + 1) {
            if(parameters[i].equals(parameter)) {
                return new ParameterBag(i, parameters[i]);
            }
        }

        return null;
    }

    @Nullable
    public static ParameterBag getCurrentParameterIndex(PsiElement psiElement) {

        if (!(psiElement.getContext() instanceof ParameterList)) {
            return null;
        }

        ParameterList parameterList = (ParameterList) psiElement.getContext();

        if (!(parameterList.getContext() instanceof MethodReference)) {
            return null;
        }

        return getCurrentParameterIndex(parameterList.getParameters(), psiElement);
    }


    @Nullable
    public static <T extends PsiElement> T getNextSiblingOfType(@Nullable PsiElement sibling, ElementPattern<PsiElement> pattern) {
        if (sibling == null) return null;
        for (PsiElement child = sibling.getNextSibling(); child != null; child = child.getNextSibling()) {
            if (pattern.accepts(child)) {
                //noinspection unchecked
                return (T)child;
            }
        }
        return null;
    }

    @Nullable
    public static <T extends PsiElement> T getPrevSiblingOfType(@Nullable PsiElement sibling, ElementPattern<PsiElement> pattern) {
        if (sibling == null) return null;
        for (PsiElement child = sibling.getPrevSibling(); child != null; child = child.getPrevSibling()) {
            if (pattern.accepts(child)) {
                //noinspection unchecked
                return (T)child;
            }
        }
        return null;
    }

}
