package fr.adrienbrault.idea.symfonyplugin.templating.variable.resolver.holder;

import com.jetbrains.php.lang.psi.elements.PhpClass;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormDataHolder {

    private PhpClass phpClass;

    public FormDataHolder(PhpClass phpClass) {
        this.phpClass = phpClass;
    }

    public PhpClass getPhpClass() {
        return phpClass;
    }

}
