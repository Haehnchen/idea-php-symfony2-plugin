package fr.adrienbrault.idea.symfony2plugin.config.php;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.codeInsight.PhpCodeInsightUtil;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.ArrayHashElement;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.php.lang.psi.elements.PhpUse;
import com.jetbrains.php.lang.psi.elements.PhpUseList;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Some helpers are also used from file-based indexers, so keep the logic purely
 * syntax-based and avoid PSI resolution or reference lookups here.
 */
public final class PhpConfigPsiUtil {
    private PhpConfigPsiUtil() {
    }

    /**
     * Returns the direct array scope: the immediate {@link ArrayHashElement} or {@link ArrayCreationExpression}
     * parent, allowing one intermediate {@link PhpPsiElement} wrapper.
     * Examples: {@code 'decorates' => 'mailer'} and {@code ['@logger']}.
     */
    @Nullable
    static PsiElement getImmediateArrayScope(@NotNull PsiElement element) {
        PsiElement parent = element.getParent();
        if (parent instanceof ArrayHashElement || parent instanceof ArrayCreationExpression) {
            return parent;
        }

        if (parent instanceof PhpPsiElement) {
            PsiElement grandParent = parent.getParent();
            if (grandParent instanceof ArrayHashElement || grandParent instanceof ArrayCreationExpression) {
                return grandParent;
            }
        }

        return null;
    }

    @Nullable
    static ArrayCreationExpression getParentArray(@NotNull PsiElement element) {
        PsiElement parentScope = getImmediateArrayScope(element);
        return parentScope instanceof ArrayCreationExpression arrayCreationExpression ? arrayCreationExpression : null;
    }

    @Nullable
    static ArrayHashElement getParentHashElement(@NotNull PsiElement element) {
        PsiElement parentScope = getImmediateArrayScope(element);
        return parentScope instanceof ArrayHashElement arrayHashElement ? arrayHashElement : null;
    }

    public static boolean isConfigFactoryCall(@NotNull MethodReference methodReference) {
        if (!"config".equals(methodReference.getName())) {
            return false;
        }

        PhpExpression classReference = methodReference.getClassReference();
        if (classReference == null) {
            return false;
        }

        String text = StringUtils.stripStart(classReference.getText(), "\\");
        return isConfigFactoryClassName(text) || isImportedConfigFactoryAlias(methodReference, text);
    }

    private static boolean isConfigFactoryClassName(@NotNull String text) {
        return "App".equals(text) || ServiceContainerUtil.CONTAINER_CONFIG_APP.substring(1).equals(text);
    }

    private static boolean isImportedConfigFactoryAlias(@NotNull MethodReference methodReference, @NotNull String text) {
        if (StringUtils.isBlank(text) || text.contains("\\")) {
            return false;
        }

        PhpPsiElement scope = PhpCodeInsightUtil.findScopeForUseOperator(methodReference);
        if (scope == null) {
            return false;
        }

        for (PhpUseList phpUseList : PhpCodeInsightUtil.collectImports(scope)) {
            for (PhpUse phpUse : phpUseList.getDeclarations()) {
                String alias = StringUtils.defaultIfBlank(phpUse.getAliasName(), phpUse.getName());
                if (text.equals(alias) && ServiceContainerUtil.CONTAINER_CONFIG_APP.equals(phpUse.getFQN())) {
                    return true;
                }
            }
        }

        return false;
    }
}
