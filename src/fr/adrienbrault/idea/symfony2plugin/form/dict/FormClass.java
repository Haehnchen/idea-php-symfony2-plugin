package fr.adrienbrault.idea.symfony2plugin.form.dict;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.jetbrains.annotations.NotNull;

public class FormClass {

    private final FormClassEnum typeEnum;
    private final PhpClass phpClass;
    private final boolean isWeak;

    public FormClass(@NotNull FormClassEnum typeEnum, @NotNull PhpClass phpClass, boolean isWeak) {
        this.typeEnum = typeEnum;
        this.phpClass = phpClass;
        this.isWeak = isWeak;
    }

    @NotNull
    public PhpClass getPhpClass() {
        return phpClass;
    }

    public boolean isWeak() {
        return isWeak;
    }

    @NotNull
    public FormClassEnum getType() {
        return typeEnum;
    }

}
