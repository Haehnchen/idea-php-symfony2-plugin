package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigBlock {
    @NotNull
    private String name;

    @NotNull
    private final PsiElement target;

    public TwigBlock(@NotNull String name, @NotNull PsiElement target) {
        this.name = name;
        this.target = target;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public PsiElement getTarget() {
        return target;
    }
}

