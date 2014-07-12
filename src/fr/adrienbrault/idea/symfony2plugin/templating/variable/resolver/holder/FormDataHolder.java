package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder;

import com.jetbrains.php.lang.psi.elements.PhpClass;

public class FormDataHolder {

    private PhpClass phpClass;

    public FormDataHolder(PhpClass phpClass) {
        this.phpClass = phpClass;
    }

    public PhpClass getPhpClass() {
        return phpClass;
    }

}
