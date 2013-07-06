package fr.adrienbrault.idea.symfony2plugin.util.controller;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ControllerReference  extends PsiReferenceBase<PsiElement> implements PsiReference {

    private StringLiteralExpression element;

    public ControllerReference(@NotNull StringLiteralExpression element) {
        super(element);
        this.element = element;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        return ControllerIndex.getControllerMethod(this.element.getProject(), element.getContents());
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return ControllerIndex.getControllerLookupElements(this.element.getProject()).toArray();
    }
}
