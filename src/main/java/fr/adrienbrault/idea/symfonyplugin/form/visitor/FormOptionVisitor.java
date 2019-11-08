package fr.adrienbrault.idea.symfonyplugin.form.visitor;

import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfonyplugin.form.dict.FormClass;
import fr.adrienbrault.idea.symfonyplugin.form.dict.FormOptionEnum;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface FormOptionVisitor {
    void visit(@NotNull PsiElement psiElement, @NotNull String option, @NotNull FormClass formClass, @NotNull FormOptionEnum optionEnum);
}
