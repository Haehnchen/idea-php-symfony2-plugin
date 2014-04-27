package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder;

import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict.QueryBuilderJoin;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict.QueryBuilderPropertyAlias;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict.QueryBuilderRelation;

import java.util.*;

public class QueryBuilderScopeContext {

    final private Set<String> parameters = new HashSet<String>();
    final private Set<String> selects = new HashSet<String>();
    final private Map<String, String> tableMap = new HashMap<String, String>();
    final private Map<String, QueryBuilderJoin> joinMap = new HashMap<String, QueryBuilderJoin>();
    final private Map<String, List<QueryBuilderRelation>> relationMap = new HashMap<String, List<QueryBuilderRelation>>();
    final private Map<String, QueryBuilderPropertyAlias> propertyAliasMap = new HashMap<String, QueryBuilderPropertyAlias>();

    public void addParameter(String parameterName) {
        this.parameters.add(parameterName);
    }

    public void addRelation(String relationName, List<QueryBuilderRelation> relations) {
        this.relationMap.put(relationName, relations);
    }

    public void addPropertyAlias(String relationName, QueryBuilderPropertyAlias propertyAlias) {
        this.propertyAliasMap.put(relationName, propertyAlias);
    }

    public void addTable(String model, String alias) {
        this.tableMap.put(model, alias);
    }

    public void addJoin(String join, QueryBuilderJoin queryBuilderJoin) {
        this.joinMap.put(join, queryBuilderJoin);
    }

    public void addSelect(String select) {
        this.selects.add(select);
    }

    public void addSelect(Collection<String> selects) {
        this.selects.addAll(selects);
    }

    public Set<String> getSelects() {
        return selects;
    }

    public Set<String> getParameters() {
        return parameters;
    }

    public Map<String, QueryBuilderJoin> getJoinMap() {
        return joinMap;
    }

    public Map<String, String> getTableMap() {
        return tableMap;
    }

    public Map<String, List<QueryBuilderRelation>> getRelationMap() {
        return relationMap;
    }

    public Map<String, QueryBuilderPropertyAlias> getPropertyAliasMap() {
        return propertyAliasMap;
    }

}
