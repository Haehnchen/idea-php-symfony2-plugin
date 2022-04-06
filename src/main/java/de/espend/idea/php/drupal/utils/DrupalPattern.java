package de.espend.idea.php.drupal.utils;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.ArrayHashElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;

public class DrupalPattern {
    public static boolean isAfterArrayKey(PsiElement psiElement, String arrayKeyName) {

        PsiElement literal = psiElement.getContext();
        if(!(literal instanceof StringLiteralExpression)) {
            return false;
        }

        PsiElement arrayValue = literal.getParent();
        if(arrayValue.getNode().getElementType() != PhpElementTypes.ARRAY_VALUE) {
            return false;
        }

        PsiElement arrayHashElement = arrayValue.getParent();
        if(!(arrayHashElement instanceof ArrayHashElement)) {
            return false;
        }

        PsiElement arrayKey = ((ArrayHashElement) arrayHashElement).getKey();
        String keyString = PhpElementsUtil.getStringValue(arrayKey);

        return arrayKeyName.equals(keyString);
    }
}
