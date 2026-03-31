package fr.adrienbrault.idea.symfony2plugin.config.php;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.ArrayHashElement;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

        PsiElement resolved = classReference.getReference() != null ? classReference.getReference().resolve() : null;
        if (resolved instanceof PhpClass phpClass) {
            return ServiceContainerUtil.CONTAINER_CONFIG_APP.equals(phpClass.getFQN());
        }

        String text = StringUtils.stripStart(classReference.getText(), "\\");
        return "App".equals(text) || ServiceContainerUtil.CONTAINER_CONFIG_APP.substring(1).equals(text);
    }
}
