package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.twig.elements.TwigElementTypes;

import java.util.regex.Pattern;

public class TwigBlock {

    private String name;
    private String shortcutName;
    private PsiFile psiFile;

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

    public PsiElement[] getBlock() {

        final String name = this.getName();
        return PsiTreeUtil.collectElements(this.psiFile, new PsiElementFilter() {
            @Override
            public boolean isAccepted(PsiElement psiElement) {

                // @TODO: move this to PlatformPatterns; withName?
                return PlatformPatterns.psiElement(TwigElementTypes.BLOCK_TAG).accepts(psiElement)
                    && Pattern.matches("\\{%[\\s+]block[\\s+]*" + Pattern.quote(name) + "[\\s+]*%}", psiElement.getText());

            }
        });
    }

}

