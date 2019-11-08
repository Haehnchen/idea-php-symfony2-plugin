package fr.adrienbrault.idea.symfonyplugin.form;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfonyplugin.form.util.FormUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormTypeReference extends PsiReferenceBase<PsiElement> implements PsiReference {

    private StringLiteralExpression element;

    public FormTypeReference(@NotNull StringLiteralExpression element) {
        super(element);
        this.element = element;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        return FormUtil.getFormTypeToClass(getElement().getProject(), element.getContents());
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return FormUtil.getFormTypeLookupElements(getElement().getProject()).toArray();
    }

}
