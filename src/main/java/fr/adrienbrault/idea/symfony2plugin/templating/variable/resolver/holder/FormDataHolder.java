package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormDataHolder {
    @NotNull
    private final PhpClass phpClass;

    @NotNull
    private final PhpClass formType;

    @Nullable
    private PsiElement field;

    public FormDataHolder(@NotNull PhpClass phpClass, @NotNull PhpClass formType) {
        this.phpClass = phpClass;
        this.formType = formType;
    }

    public FormDataHolder(@NotNull PhpClass phpClass, @NotNull PhpClass formType, @NotNull PsiElement field) {
        this.phpClass = phpClass;
        this.formType = formType;
        this.field = field;
    }

    @NotNull
    public PhpClass getPhpClass() {
        return phpClass;
    }

    @NotNull
    public PhpClass getFormType() {
        return formType;
    }

    @Nullable
    public PsiElement getField() {
        return this.field;
    }
}
