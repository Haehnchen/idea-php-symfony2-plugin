package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict.QueryBuilderJoin;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict.QueryBuilderRelation;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* Created by daniel on 27.04.14.
*/
class Resolver {

    final private Project project;
    final private Map<String, String> joins = new HashMap<String, String>();
    final private Map<String, List<QueryBuilderRelation>> relationMap;
    final private Map<String, QueryBuilderJoin> joinMap = new HashMap<String, QueryBuilderJoin>();

    public Resolver(Project project, String aliasRoot, String aliasClass, Map<String, List<QueryBuilderRelation>> relationMap, Map<String, QueryBuilderJoin> joinMap) {
        this.project = project;
        this.joins.put(aliasRoot, aliasClass);
        this.relationMap = relationMap;
        this.joinMap.putAll(joinMap);
    }

    public void collect() {

        for(QueryBuilderJoin queryBuilderJoin: joinMap.values()) {
            if(queryBuilderJoin.getResolvedClass() == null) {
                String joinName = queryBuilderJoin.getJoin();
                String[] split = StringUtils.split(joinName, ".");
                if(split.length == 2) {

                    // alias.foreignProperty
                    String foreignJoinAlias = split[0];
                    String foreignJoinField = split[1];

                    if(relationMap.containsKey(foreignJoinAlias)) {
                        for(QueryBuilderRelation queryBuilderRelation: relationMap.get(foreignJoinAlias)) {
                            if(queryBuilderRelation.getFieldName().equals(foreignJoinField)) {
                                String targetEntity = queryBuilderRelation.getTargetEntity();
                                if(targetEntity != null) {
                                    PhpClass phpClass = PhpElementsUtil.getClassInterface(project, targetEntity);
                                    if(phpClass != null) {
                                        relationMap.put(queryBuilderJoin.getAlias(), QueryBuilderParser.attachRelationFields(phpClass));
                                        queryBuilderJoin.setResolvedClass(targetEntity);
                                        collect();
                                    }
                                }

                            }
                        }

                    }

                }


            }
        }

    }

}
