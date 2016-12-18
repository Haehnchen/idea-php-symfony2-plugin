package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigBlock {

    @NotNull
    private String name;

    @NotNull
    private final PsiElement target;

    @Nullable
    private String shortcutName;

    public TwigBlock(@NotNull String name, @NotNull PsiElement target) {
        this.name = name;
        this.target = target;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @Nullable
    public String getShortcutName() {
        return shortcutName;
    }

    @NotNull
    public PsiFile getPsiFile() {
        return target.getContainingFile();
    }

    @NotNull
    public PsiElement[] getBlock() {
        return new PsiElement[] {target};
    }

    public void setShortcutName(@Nullable String shortcutName) {
        // @TODO: remove this
        this.shortcutName = shortcutName;
    }

}

