package fr.adrienbrault.idea.symfonyplugin.form.dict;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfonyplugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormTypeClass {

    final private String name;
    private PhpClass phpClass;
    private String phpClassName;
    final private EnumFormTypeSource source;

    public FormTypeClass(@NotNull String name, @NotNull PhpClass phpClass, @NotNull EnumFormTypeSource source) {
        this.name = name;
        this.phpClass = phpClass;
        this.source = source;
    }

    public FormTypeClass(@NotNull String name, @NotNull String phpClassName, @NotNull EnumFormTypeSource source) {
        this.name = name;
        this.phpClassName = phpClassName;
        this.source = source;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @Nullable
    public PhpClass getPhpClass() {
        return phpClass;
    }

    @Nullable
    public PhpClass getPhpClass(Project project) {

        if(phpClass != null) {
            return phpClass;
        }

        if(this.phpClassName == null) {
            return null;
        }

        return PhpElementsUtil.getClass(project, this.phpClassName);
    }

    @NotNull
    public EnumFormTypeSource getSource() {
        return source;
    }

    public String getPhpClassName() {
        return phpClassName;
    }

}
