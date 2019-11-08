package fr.adrienbrault.idea.symfonyplugin.dic.container.dict;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.Method;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceTypeHint {
    @NotNull
    private final Method method;

    private final int index;

    @NotNull
    private final PsiElement psiElement;

    public ServiceTypeHint(@NotNull Method method, int index, @NotNull PsiElement psiElement) {
        this.method = method;
        this.index = index;
        this.psiElement = psiElement;
    }

    @NotNull
    public Method getMethod() {
        return method;
    }

    public int getIndex() {
        return index;
    }

    @NotNull
    public PsiElement getElement() {
        return psiElement;
    }
}
