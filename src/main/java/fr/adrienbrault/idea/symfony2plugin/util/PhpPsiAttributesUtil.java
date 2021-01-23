package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * Helpers for PHP 8 Attributes psi access
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpPsiAttributesUtil {
    @Nullable
    public static String getAttributeValueByNameAsString(@NotNull PhpAttribute attribute, @NotNull String attributeName) {
        PsiElement nextSibling = findAttributeByName(attribute, attributeName);

        if (nextSibling instanceof StringLiteralExpression) {
            String contents = ((StringLiteralExpression) nextSibling).getContents();
            if (StringUtils.isNotBlank(contents)) {
                return contents;
            }
        }

        return null;
    }

    @NotNull
    public static Collection<String> getAttributeValueByNameAsArray(@NotNull PhpAttribute attribute, @NotNull String attributeName) {
        PsiElement nextSibling = findAttributeByName(attribute, attributeName);

        if (nextSibling instanceof ArrayCreationExpression) {
            return PhpElementsUtil.getArrayValuesAsString((ArrayCreationExpression) nextSibling);
        }

        return Collections.emptyList();
    }

    /**
     * Workaround to find given attribute: "#[Route('/attributesWithoutName', name: "")]" as attribute iteration given the index as "int" but not the key as name
     */
    @Nullable
    private static PsiElement findAttributeByName(@NotNull PhpAttribute attribute, @NotNull String attributeName) {
        ParameterList parameterList = PsiTreeUtil.findChildOfType(attribute, ParameterList.class);
        if (parameterList == null) {
            return null;
        }

        Collection<PsiElement> childrenOfTypeAsList = PsiElementUtils.getChildrenOfTypeAsList(parameterList, getAttributeColonPattern(attributeName));

        if (childrenOfTypeAsList.isEmpty()) {
            return null;
        }

        PsiElement colon = childrenOfTypeAsList.iterator().next();

        return PhpPsiUtil.getNextSibling(colon, psiElement -> psiElement instanceof PsiWhiteSpace);
    }

    /**
     * "#[Route('/path', name: 'attributes_action')]"
     */
    @NotNull
    private static PsiElementPattern.Capture<PsiElement> getAttributeColonPattern(String name) {
        return PlatformPatterns.psiElement().withElementType(
            PhpTokenTypes.opCOLON
        ).afterLeaf(PlatformPatterns.psiElement().withElementType(PhpTokenTypes.IDENTIFIER).withText(name));
    }
}
