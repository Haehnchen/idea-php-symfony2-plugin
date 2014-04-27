package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict;

import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class QueryBuilderPropertyAlias {

    final private String alias;
    final private String fieldName;
    final private Collection<PsiElement> psiTargets = new ArrayList<PsiElement>();
    private DoctrineModelField field;

    public QueryBuilderPropertyAlias(String alias, String fieldName, DoctrineModelField field) {
        this.alias = alias;
        this.fieldName = fieldName;
        this.field = field;
    }

    public QueryBuilderPropertyAlias(String alias, String fieldName) {
        this.alias = alias;
        this.fieldName = fieldName;
        this.psiTargets.addAll(psiTargets);
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getAlias() {
        return alias;
    }

    public Collection<PsiElement> getPsiTargets() {
        return field == null ? Collections.EMPTY_LIST : field.getTargets();
    }

    @Nullable
    public DoctrineModelField getField() {
        return field;
    }

}
