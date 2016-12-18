package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict;

import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class QueryBuilderRelation {

    private final String fieldName;
    private String targetEntity;

    public QueryBuilderRelation(String fieldName) {
        this.fieldName = fieldName;
    }

    public QueryBuilderRelation(String fieldName, String targetEntity) {
        this.fieldName = fieldName;
        this.targetEntity = targetEntity;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Nullable
    public String getTargetEntity() {
        return targetEntity;
    }

}
