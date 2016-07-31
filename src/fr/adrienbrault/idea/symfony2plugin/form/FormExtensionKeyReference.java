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
public class FormExtensionKeyReference extends PsiReferenceBase<PsiElement> implements PsiReference {

    private StringLiteralExpression element;

    private String[] formTypes = new String[] {
        "form",
        "Symfony\\Component\\Form\\Extension\\Core\\Type\\FormType",
    };

    public FormExtensionKeyReference(@NotNull StringLiteralExpression element) {
        super(element);
        this.element = element;
    }

    @Nullable
    @Override
    public PsiElement resolve() {

        Collection<PsiElement> targets = FormOptionsUtil.getFormExtensionsKeysTargets(element, formTypes);
        if(targets.size() > 0) {
            return targets.iterator().next();
        }

        return null;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return FormOptionsUtil.getFormExtensionKeysLookupElements(getElement().getProject(), formTypes).toArray();
    }

}
