package fr.adrienbrault.idea.symfony2plugin.util.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PsiElementAssertUtil {

    public static boolean isNotNullAndIsElementType(@Nullable PsiElement psiElement, @NotNull IElementType iElementType) {
        return psiElement != null && psiElement.getNode().getElementType() == iElementType;
    }

    public static <T extends PsiElement> T getParentOfTypeOrNull(@NotNull PsiElement element, @NotNull Class<T> aClass) {
        PsiElement parent = element.getParent();
        return aClass.isInstance(parent) ? (T) parent : null;
    }
}