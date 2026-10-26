package fr.adrienbrault.idea.symfony2plugin.form.dict;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormTypeClass {

    public enum Type {
        PLAIN, ATTRIBUTE
    }

    private final String name;
    private PhpClass phpClass;
    private final String phpClassName;
    private final EnumFormTypeSource source;
    private final Type type;

    public FormTypeClass(@NotNull String name, @NotNull String phpClassName, @NotNull EnumFormTypeSource source, @NotNull Type type) {
        this.name = name;
        this.phpClassName = phpClassName;
        this.source = source;
        this.type = type;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @Nullable
    public PhpClass getPhpClass(@NotNull Project project) {
        if(phpClass != null) {
            return phpClass;
        }

        return this.phpClass = PhpElementsUtil.getClass(project, this.phpClassName);
    }

    @NotNull
    public EnumFormTypeSource getSource() {
        return source;
    }

    @NotNull
    public Type getType() {
        return type;
    }

    public String getPhpClassName() {
        return phpClassName;
    }
}
