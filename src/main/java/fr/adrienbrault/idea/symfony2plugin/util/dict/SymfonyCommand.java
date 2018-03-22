package fr.adrienbrault.idea.symfony2plugin.util.dict;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyCommand {

    @NotNull
    private final String name;

    @NotNull
    private final PsiElement psiElement;

    public SymfonyCommand(@NotNull String name, @NotNull PsiElement psiElement) {
        this.name = name;
        this.psiElement = psiElement;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public PsiElement getPsiElement() {
        return psiElement;
    }

}
