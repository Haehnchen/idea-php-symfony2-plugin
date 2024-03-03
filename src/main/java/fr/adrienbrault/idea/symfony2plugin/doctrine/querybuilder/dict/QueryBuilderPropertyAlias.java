package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict;

import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class QueryBuilderPropertyAlias {

    final private String alias;
    final private String fieldName;
    private final DoctrineModelField field;

    public QueryBuilderPropertyAlias(String alias, String fieldName, DoctrineModelField field) {
        this.alias = alias;
        this.fieldName = fieldName;
        this.field = field;
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
