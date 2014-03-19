package fr.adrienbrault.idea.symfony2plugin.doctrine.dict;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class DoctrineModelField {

    private final String name;
    private String typeName;

    public DoctrineModelField(String name) {
        this.name = name;
    }

    public DoctrineModelField(String name, String typeName) {
        this(name);
        this.typeName = typeName;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    public String getTypeName() {
        return this.typeName;
    }

}
