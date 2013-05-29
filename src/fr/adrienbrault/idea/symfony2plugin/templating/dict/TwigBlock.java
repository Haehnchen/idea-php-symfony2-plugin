package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import com.intellij.psi.PsiFile;
import com.jetbrains.twig.TwigFile;

public class TwigBlock {

    private String name;
    private String shortcutName;
    private PsiFile psiFile;

    public TwigBlock(String name, PsiFile psiFile) {
        this.name = name;
        this.psiFile = psiFile;
    }

    public TwigBlock(String name, String shortCutName, PsiFile psiFile) {
        this.name = name;
        this.shortcutName = shortCutName;
        this.psiFile = psiFile;
    }

    public String getName() {
        return name;
    }

    public String getShortcutName() {
        return shortcutName;
    }

    public PsiFile getPsiFile() {
        return psiFile;
    }

}

