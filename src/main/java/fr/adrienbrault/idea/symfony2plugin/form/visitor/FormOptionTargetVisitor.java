package fr.adrienbrault.idea.symfony2plugin.form.visitor;

import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormClass;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormOptionEnum;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormOptionTargetVisitor implements FormOptionVisitor {
    @NotNull
    private final String optionName;

    @NotNull
    private final Collection<PsiElement> psiElements;

    public FormOptionTargetVisitor(@NotNull String optionName, @NotNull Collection<PsiElement> psiElements) {
        this.optionName = optionName;
        this.psiElements = psiElements;
    }

    @Override
    public void visit(@NotNull PsiElement psiElement, @NotNull String option, @NotNull FormClass formClass, @NotNull FormOptionEnum optionEnum) {
        if(option.equalsIgnoreCase(optionName)) {
            psiElements.add(psiElement);
        }
    }
}
