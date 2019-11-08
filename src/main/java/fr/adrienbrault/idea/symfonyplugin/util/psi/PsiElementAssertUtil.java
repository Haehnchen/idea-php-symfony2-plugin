package fr.adrienbrault.idea.symfonyplugin.util.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PsiElementAssertUtil {

    public static boolean isNotNullAndIsElementType(@Nullable PsiElement psiElement, @NotNull IElementType... iElementType) {
        return psiElement != null && Arrays.asList(iElementType).contains(psiElement.getNode().getElementType());
    }

    @Nullable
    public static <T extends PsiElement> T getParentOfTypeOrNull(@NotNull PsiElement element, @NotNull Class<T> aClass) {
        PsiElement parent = element.getParent();
        return aClass.isInstance(parent) ? (T) parent : null;
    }

    @Nullable
    public static <T extends PsiNamedElement> T getParentOfTypeWithNameOrNull(@NotNull PsiElement element, @NotNull Class<T> aClass, @NotNull String name) {
        PsiElement parent = element.getParent();
        if(!aClass.isInstance(parent) || !(parent instanceof PsiNamedElement) || !name.equals(((PsiNamedElement) parent).getName())) {
            return null;
        }

        return (T) parent;
    }

    @Nullable
    public static <T extends PsiElement> T getInstanceOfOrNull(@Nullable PsiElement element, @NotNull Class<T> aClass) {
        if(element == null) return null;
        return aClass.isInstance(element) ? (T) element : null;
    }

}
