package fr.adrienbrault.idea.symfony2plugin.form.dict;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpClass;

public class FormTypeClass {

    final private String name;
    final private PhpClass phpClass;
    final private PsiElement returnPsiElement;

    public FormTypeClass(String name, PhpClass phpClass, PsiElement returnPsiElement) {
        this.name = name;
        this.phpClass = phpClass;
        this.returnPsiElement = returnPsiElement;
    }

    public String getName() {
        return name;
    }

    public PhpClass getPhpClass() {
        return phpClass;
    }

    public PsiElement getReturnPsiElement() {
        return returnPsiElement;
    }

}
