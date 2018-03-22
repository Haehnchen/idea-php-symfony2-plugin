package fr.adrienbrault.idea.symfony2plugin.form.dict;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormOption {
    @NotNull
    private final String option;

    @NotNull
    private final FormClass formClass;

    @NotNull
    private final Collection<PsiElement> psiElements = new HashSet<>();

    @NotNull
    private final Collection<FormOptionEnum> optionEnum = new HashSet<>();

    public FormOption(@NotNull String option, @NotNull FormClass formClass, @NotNull FormOptionEnum optionEnum, @NotNull PsiElement psiElement) {
        this.option = option;
        this.formClass = formClass;

        this.psiElements.add(psiElement);
        this.optionEnum.add(optionEnum);
    }

    @NotNull
    public String getOption() {
        return option;
    }

    @NotNull
    public FormClass getFormClass() {
        return formClass;
    }

    @NotNull
    public Collection<FormOptionEnum> getOptionEnum() {
        return optionEnum;
    }

    public void addOptionEnum(@NotNull FormOptionEnum optionEnum) {
        this.optionEnum.add(optionEnum);
    }

    public void addTarget(@NotNull PsiElement psiElement) {
        this.psiElements.add(psiElement);
    }

    @NotNull
    public Collection<PsiElement> getPsiTargets() {
        return psiElements;
    }
}
