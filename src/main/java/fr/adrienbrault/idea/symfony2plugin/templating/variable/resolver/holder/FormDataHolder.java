package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormDataHolder {
    @NotNull
    private final PhpClass phpClass;

    @NotNull
    private final PhpClass formType;

    public FormDataHolder(@NotNull PhpClass phpClass, @NotNull PhpClass formType) {
        this.phpClass = phpClass;
        this.formType = formType;
    }

    @NotNull
    public PhpClass getPhpClass() {
        return phpClass;
    }

    @NotNull
    public PhpClass getFormType() {
        return formType;
    }
}
