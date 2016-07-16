package fr.adrienbrault.idea.symfony2plugin.form.dict;

import org.jetbrains.annotations.NotNull;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormOption {

    private final String option;
    private final FormClass formClass;

    @NotNull
    private final Collection<FormOptionEnum> optionEnum = new HashSet<>();

    public FormOption(@NotNull String option, @NotNull FormClass formClass) {
        this.option = option;
        this.formClass = formClass;
        this.optionEnum.add(FormOptionEnum.DEFAULT);
    }

    public FormOption(@NotNull String option, @NotNull FormClass formClass, @NotNull FormOptionEnum optionEnum) {
        this.option = option;
        this.formClass = formClass;
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
    @NotNull
    public FormOption addOptionEnum(@NotNull FormOptionEnum optionEnum) {
        this.optionEnum.add(optionEnum);
        return this;
    }

}
