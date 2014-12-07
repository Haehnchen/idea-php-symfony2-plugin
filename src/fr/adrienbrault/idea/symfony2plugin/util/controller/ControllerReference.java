package fr.adrienbrault.idea.symfony2plugin.util.controller;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ControllerReference extends PsiReferenceBase<PsiElement> implements PsiReference {

    private StringLiteralExpression element;

    public ControllerReference(@NotNull StringLiteralExpression element) {
        super(element);
        this.element = element;
    }

    @Nullable
    @Override
    public PsiElement resolve() {

        // @TODO: multiresolve

        String contents = element.getContents();
        if(StringUtils.isBlank(contents)) {
            return null;
        }

        PsiElement[] methods = RouteHelper.getMethodsOnControllerShortcut(this.element.getProject(), contents);
        if(methods.length > 0) {
            return methods[0];
        }

        return null;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return ControllerIndex.getControllerLookupElements(this.element.getProject()).toArray();
    }
}
