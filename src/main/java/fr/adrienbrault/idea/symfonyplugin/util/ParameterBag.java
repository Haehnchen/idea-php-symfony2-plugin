package fr.adrienbrault.idea.symfonyplugin.util;

import com.intellij.psi.PsiElement;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ParameterBag {

    private int index;
    private PsiElement psiElement;

    public ParameterBag(int index, PsiElement psiElement) {
        this.index = index;
        this.psiElement = psiElement;
    }

    public int getIndex() {
        return index;
    }

    public String getValue() {
        return PsiElementUtils.getMethodParameter(psiElement);
    }

    public PsiElement getElement() {
        return this.psiElement;
    }

}
