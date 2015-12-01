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

}
