package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict.QueryBuilderClassJoin;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict.QueryBuilderJoin;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict.QueryBuilderRelation;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class QueryBuilderRelationClassResolver {

    final private Project project;
    final private Map<String, String> joins = new HashMap<>();
    final private Map<String, List<QueryBuilderRelation>> relationMap;
    final private Map<String, QueryBuilderJoin> joinMap = new HashMap<>();

    final private Map<String, QueryBuilderClassJoin> joinMapClass = new HashMap<>();

    public QueryBuilderRelationClassResolver(Project project, String aliasRoot, String aliasClass, Map<String, List<QueryBuilderRelation>> relationMap, Map<String, QueryBuilderJoin> joinMap, Map<String, QueryBuilderClassJoin> joinMapClass) {
        this.project = project;
        this.joins.put(aliasRoot, aliasClass);
        this.relationMap = relationMap;
        this.joinMap.putAll(joinMap);
        this.joinMapClass.putAll(joinMapClass);
    }

    public void collect() {
        // $qb->join(\App\Entity\Car::class, 'car');
        for (QueryBuilderClassJoin queryBuilderJoin: joinMapClass.values()) {
            PhpClass phpClass = PhpElementsUtil.getClassInterface(project, queryBuilderJoin.className());
            if (phpClass != null) {
                relationMap.put(queryBuilderJoin.alias(), QueryBuilderMethodReferenceParser.attachRelationFields(phpClass));
            }
        }

        // $qb->join('fooBar.bird', 'bird');
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
                                        relationMap.put(queryBuilderJoin.getAlias(), QueryBuilderMethodReferenceParser.attachRelationFields(phpClass));
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
