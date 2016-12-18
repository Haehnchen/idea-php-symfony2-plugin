package fr.adrienbrault.idea.symfony2plugin.doctrine.dict;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineModelField {

    private String relation;
    private String relationType;
    private final String name;
    private String typeName;
    private Collection<PsiElement> targets = new ArrayList<>();
    private String column;

    public DoctrineModelField setTypeName(String typeName) {
        this.typeName = typeName;
        return this;
    }

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

    @Nullable
    public String getTypeName() {
        return this.typeName;
    }

    @NotNull
    public Collection<PsiElement> getTargets() {
        return targets;
    }

    public DoctrineModelField addTarget(PsiElement target) {
        this.targets.add(target);
        return this;
    }

    @Nullable
    public String getRelation() {
        return relation;
    }

    public DoctrineModelField setRelation(String relation) {
        this.relation = relation;
        return this;
    }

    public String getRelationType() {
        return relationType;
    }

    public DoctrineModelField setRelationType(String relationType) {
        this.relationType = relationType;
        return this;
    }

    @Nullable
    public String getColumn() {
        return column;
    }

    public void setColumn(@Nullable String column) {
        this.column = column;
    }

}
