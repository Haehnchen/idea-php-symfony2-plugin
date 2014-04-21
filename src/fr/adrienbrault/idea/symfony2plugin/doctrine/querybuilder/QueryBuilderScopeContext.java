package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder;

import java.util.*;

public class QueryBuilderScopeContext {

    final private Set<String> parameters = new HashSet<String>();
    final private Map<String, String> tableMap = new HashMap<String, String>();
    final private Map<String, QueryBuilderParser.QueryBuilderJoin> joinMap = new HashMap<String, QueryBuilderParser.QueryBuilderJoin>();
    final private Map<String, List<QueryBuilderParser.QueryBuilderRelation>> relationMap = new HashMap<String, List<QueryBuilderParser.QueryBuilderRelation>>();
    final private Map<String, QueryBuilderParser.QueryBuilderPropertyAlias> propertyAliasMap = new HashMap<String, QueryBuilderParser.QueryBuilderPropertyAlias>();

    public void addParameter(String parameterName) {
        this.parameters.add(parameterName);
    }

    public void addRelation(String relationName, List<QueryBuilderParser.QueryBuilderRelation> relations) {
        this.relationMap.put(relationName, relations);
    }

    public void addPropertyAlias(String relationName, QueryBuilderParser.QueryBuilderPropertyAlias propertyAlias) {
        this.propertyAliasMap.put(relationName, propertyAlias);
    }

    public void addTable(String model, String alias) {
        this.tableMap.put(model, alias);
    }

    public void addJoin(String join, QueryBuilderParser.QueryBuilderJoin queryBuilderJoin) {
        this.joinMap.put(join, queryBuilderJoin);
    }

    public Set<String> getParameters() {
        return parameters;
    }

    public Map<String, QueryBuilderParser.QueryBuilderJoin> getJoinMap() {
        return joinMap;
    }

    public Map<String, String> getTableMap() {
        return tableMap;
    }

    public Map<String, List<QueryBuilderParser.QueryBuilderRelation>> getRelationMap() {
        return relationMap;
    }

    public Map<String, QueryBuilderParser.QueryBuilderPropertyAlias> getPropertyAliasMap() {
        return propertyAliasMap;
    }

}
