package fr.adrienbrault.idea.symfony2plugin.form.dict;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.Nullable;

public class FormTypeClass {

    final private String name;
    private PhpClass phpClass;
    private String phpClassName;
    private PsiElement returnPsiElement;
    final private EnumFormTypeSource source;

    public FormTypeClass(String name, PhpClass phpClass, PsiElement returnPsiElement, EnumFormTypeSource source) {
        this.name = name;
        this.phpClass = phpClass;
        this.returnPsiElement = returnPsiElement;
        this.source = source;
    }

    public FormTypeClass(String name, String phpClassName, EnumFormTypeSource source) {
        this.name = name;
        this.phpClassName = phpClassName;
        this.source = source;
    }

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

    @Nullable
    public PsiElement getReturnPsiElement() {
        return returnPsiElement;
    }

    public EnumFormTypeSource getSource() {
        return source;
    }

    public String getPhpClassName() {
        return phpClassName;
    }

}
