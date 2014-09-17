package fr.adrienbrault.idea.symfony2plugin.form;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormDefaultOptionsKeyReference extends PsiReferenceBase<PsiElement> implements PsiReference {

    private StringLiteralExpression element;
    private String formType;

    public FormDefaultOptionsKeyReference(@NotNull StringLiteralExpression element, String formType) {
        super(element);
        this.element = element;
        this.formType = formType;
    }

    @Nullable
    @Override
    public PsiElement resolve() {

        Collection<PsiElement> defaultOptionTargets = FormOptionsUtil.getDefaultOptionTargets(element, this.formType);
        if(defaultOptionTargets.size() > 0) {
            return defaultOptionTargets.iterator().next();
        }

        return null;
    }


    @NotNull
    @Override
    public Object[] getVariants() {
        return FormOptionsUtil.getDefaultOptionLookupElements(getElement().getProject(), this.formType).toArray();
    }

}
