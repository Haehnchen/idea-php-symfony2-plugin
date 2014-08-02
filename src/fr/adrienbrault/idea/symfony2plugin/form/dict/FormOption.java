package fr.adrienbrault.idea.symfony2plugin.form.dict;

public class FormOption {

    private final String option;
    private final FormClass formClass;

    public FormOption(String option, FormClass formClass) {
        this.option = option;
        this.formClass = formClass;
    }

    public String getOption() {
        return option;
    }

    public FormClass getFormClass() {
        return formClass;
    }

}
