package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiElementFilter;
import org.intellij.lang.annotations.RegExp;

public  class RegexPsiElementFilter implements PsiElementFilter {

    private Class aClass;
    private IElementType elementType;


    @RegExp
    private String regex;

    public <T extends PsiElement> RegexPsiElementFilter(Class<T> aClass, @RegExp String regex) {
        this.aClass = aClass;
        this.regex = regex;
    }

    public RegexPsiElementFilter(IElementType elementType, @RegExp String regex) {
        this.elementType = elementType;
        this.regex = regex;
    }

    @Override
    public boolean isAccepted(PsiElement psiElement) {
        if(this.aClass != null) {
            return PlatformPatterns.psiElement(this.aClass).accepts(psiElement)
                && psiElement.getText().matches(this.regex);
        }

        return PlatformPatterns.psiElement(this.elementType).accepts(psiElement)
            && psiElement.getText().matches(this.regex);
    }

}