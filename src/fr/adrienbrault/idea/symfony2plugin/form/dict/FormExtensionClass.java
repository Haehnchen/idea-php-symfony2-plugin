package fr.adrienbrault.idea.symfony2plugin.form.dict;

import com.jetbrains.php.lang.psi.elements.PhpClass;

public class FormExtensionClass {

    private final PhpClass phpClass;
    private final boolean isWeak;

    public FormExtensionClass(PhpClass phpClass, boolean isWeak) {
        this.phpClass = phpClass;
        this.isWeak = isWeak;
    }

    public PhpClass getPhpClass() {
        return phpClass;
    }

    public boolean isWeak() {
        return isWeak;
    }

}
