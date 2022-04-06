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
import com.jetbrains.php.lang.psi.stubs.indexes.expectedArguments.PhpExpectedFunctionArgument;
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
     * find default "#[Route(path: '/attributesWithoutName')]" or "#[Route('/attributesWithoutName')]"
     */
    @Nullable
    public static String getAttributeValueByNameAsStringWithDefaultParameterFallback(@NotNull PhpAttribute attribute, @NotNull String attributeName) {
        String pathAttribute = PhpPsiAttributesUtil.getAttributeValueByNameAsString(attribute, attributeName);
        if (StringUtils.isNotBlank(pathAttribute)) {
            return pathAttribute;
        }

        // find default "#[Route('/attributesWithoutName')]"
        for (PhpAttribute.PhpAttributeArgument argument : attribute.getArguments()) {
            PhpExpectedFunctionArgument argument1 = argument.getArgument();
            if (argument1.getArgumentIndex() == 0) {
                String value = PsiElementUtils.trimQuote(argument1.getValue());
                if (StringUtils.isNotBlank(value)) {
                    return value;
                }
            }
        }

        return null;
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
