package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.completion.ConstantEnumCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryBuilderParser  {

    private boolean collectParameter = true;
    private boolean collectFrom = true;
    private boolean collectJoins = true;
    private boolean collectProperties = true;
    private Project project;

    final private Collection<MethodReference> methodReferences;

    public QueryBuilderParser(Project project, Collection<MethodReference> methodReferences) {
        this.methodReferences = methodReferences;
        this.project = project;
    }

    public QueryBuilderScopeContext collect() {
        QueryBuilderScopeContext qb = new QueryBuilderScopeContext();

        for(MethodReference methodReference: methodReferences) {

            String name = methodReference.getName();
            if(name == null) {
                continue;
            }

            if(collectParameter == true) {
                if(name.equals("where") || name.equals("andWhere")) {
                    String value = PsiElementUtils.getMethodParameterAt(methodReference, 0);
                    if(value != null) {
                        Matcher matcher = Pattern.compile(":(\\w+)", Pattern.MULTILINE).matcher(value);
                        while(matcher.find()){
                            qb.addParameter(matcher.group(1));
                        }
                    }
                }
            }

            if(collectFrom == true) {
                if(name.equals("from")) {
                    String table = PsiElementUtils.getMethodParameterAt(methodReference, 0);
                    String alias = PsiElementUtils.getMethodParameterAt(methodReference, 1);
                    if(table != null && alias != null) {
                        qb.addTable(table, alias);
                    }

                }
            }

            if(collectJoins == true) {
                if(name.equals("join") || name.equals("leftJoin") || name.equals("rightJoin") || name.equals("innerJoin")) {
                    String join = PsiElementUtils.getMethodParameterAt(methodReference, 0);
                    String alias = PsiElementUtils.getMethodParameterAt(methodReference, 1);
                    if(join != null && alias != null) {
                        qb.addJoin(alias, new QueryBuilderJoin(join, alias));
                    }
                }
            }

        }

        if(qb.getTableMap().size() > 0) {
            Map.Entry<String, String> entry = qb.getTableMap().entrySet().iterator().next();
            String className = entry.getKey();
            PhpClass phpClass = PhpElementsUtil.getClassInterface(project, className);
            qb.addRelation(entry.getValue(), attachRelationFields(phpClass));

            Resolver resolver = new Resolver(project, entry.getValue(), entry.getKey(), qb.getRelationMap(), qb.getJoinMap());
            resolver.collect();

        }

        if(collectProperties == true) {
            for(QueryBuilderJoin join: qb.getJoinMap().values()) {
                String className = join.getResolvedClass();
                if(className != null) {
                    PhpClass phpClass = PhpElementsUtil.getClassInterface(project, className);
                    if(phpClass != null) {
                        for(Field field: phpClass.getFields()) {
                            if(!field.isConstant()) {
                                qb.addPropertyAlias(join.getAlias() + "." + field.getName(), new QueryBuilderPropertyAlias(join.getAlias(), field.getName(), field));
                            }
                        }
                    }
                }
            }
        }

        return qb;

    }

    private static class Resolver {

        final private Project project;
        final private Map<String, String> joins = new HashMap<String, String>();
        final private Map<String, List<QueryBuilderParser.QueryBuilderRelation>> relationMap;
        final private Map<String, QueryBuilderParser.QueryBuilderJoin> joinMap = new HashMap<String, QueryBuilderJoin>();

        public Resolver(Project project, String aliasRoot, String aliasClass, Map<String, List<QueryBuilderParser.QueryBuilderRelation>> relationMap, Map<String, QueryBuilderParser.QueryBuilderJoin> joinMap) {
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
                                if(queryBuilderRelation.fieldName.equals(foreignJoinField)) {
                                    String targetEntity = queryBuilderRelation.getTargetEntity();
                                    if(targetEntity != null) {
                                        PhpClass phpClass = PhpElementsUtil.getClassInterface(project, targetEntity);
                                        if(phpClass != null) {
                                            relationMap.put(queryBuilderJoin.getAlias(), attachRelationFields(phpClass));
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

    static private List<QueryBuilderRelation> attachRelationFields(PhpClass phpClass) {

        List<QueryBuilderRelation> relations = new ArrayList<QueryBuilderRelation>();

        for(Field field: phpClass.getFields()) {
            if(!field.isConstant()) {
                attachRelationFields(field, relations);
            }
        }

        return relations;
    }

    static private void attachRelationFields(Field field, List<QueryBuilderRelation> relationFields) {

        PhpDocComment docBlock = field.getDocComment();
        if(docBlock == null) {
            return;
        }

        String text = docBlock.getText();

        // targetEntity name
        String targetEntity = null;
        Matcher matcher = Pattern.compile("targetEntity=[\"|']([\\w_\\\\]+)[\"|']").matcher(text);
        if (matcher.find()) {
            targetEntity = matcher.group(1);
        }

        // relation type
        matcher = Pattern.compile("((Many|One)To(Many|One))\\(").matcher(text);
        if (matcher.find()) {
            if(targetEntity != null) {
                relationFields.add(new QueryBuilderRelation(field.getName(), targetEntity));
            } else {
                relationFields.add(new QueryBuilderRelation(field.getName()));
            }

        }

    }

    public static class QueryBuilderPropertyAlias {

        final private String alias;
        final private String fieldName;
        final private PsiElement psiTarget;

        public QueryBuilderPropertyAlias(String alias, String fieldName, PsiElement psiTarget) {
            this.alias = alias;
            this.fieldName = fieldName;
            this.psiTarget = psiTarget;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getAlias() {
            return alias;
        }

        public PsiElement getPsiTarget() {
            return psiTarget;
        }

    }

    public static class QueryBuilderJoin {

        final private String join;
        final private String alias;
        private String resolvedClass;

        public QueryBuilderJoin(String join, String alias) {
            this.join = join;
            this.alias = alias;
        }

        public String getAlias() {
            return alias;
        }

        public String getJoin() {
            return join;
        }

        @Nullable
        public String getResolvedClass() {
            return resolvedClass;
        }

        public void setResolvedClass(String resolvedClass) {
            this.resolvedClass = resolvedClass;
        }

    }

    public static class QueryBuilderRelation {

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

}

