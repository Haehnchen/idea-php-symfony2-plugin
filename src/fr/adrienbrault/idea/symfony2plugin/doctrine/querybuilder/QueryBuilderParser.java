package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.completion.ConstantEnumCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityReference;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.DoctrineModel;
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

    private Map<String, String> findRootDefinition(Collection<MethodReference> methodReferences) {

        Map<String, String> roots = new HashMap<String, String>();

        if(methodReferences.size() == 0) {
            return roots;
        }

        String rootAlias = null;
        String repository = null;

        // getRepository('Foo')->createQueryBuilder('test');
        for(MethodReference methodReference: methodReferences) {
            String methodReferenceName = methodReference.getName();

            if("createQueryBuilder".equals(methodReferenceName)) {
                String possibleAlias = PhpElementsUtil.getStringValue(PsiElementUtils.getMethodParameterPsiElementAt(methodReference, 0));
                if(possibleAlias != null) {
                    rootAlias = possibleAlias;
                }
            }

            if("getRepository".equals(methodReferenceName)) {
                String possibleRepository = PhpElementsUtil.getStringValue(PsiElementUtils.getMethodParameterPsiElementAt(methodReference, 0));
                if(possibleRepository != null) {
                    repository = possibleRepository;
                    PhpClass phpClass = EntityHelper.resolveShortcutName(project, repository);
                    if(phpClass != null) {
                        repository = phpClass.getPresentableFQN();
                    }
                }
            }

            // $qb->from('Foo\Class', 'article')
            if("from".equals(methodReferenceName)) {
                String table = PhpElementsUtil.getStringValue(PsiElementUtils.getMethodParameterPsiElementAt(methodReference, 0));
                String alias = PhpElementsUtil.getStringValue(PsiElementUtils.getMethodParameterPsiElementAt(methodReference, 1));
                if(table != null && alias != null) {
                    roots.put(table, alias);
                }
            }

        }

        if(rootAlias != null && repository != null) {
            roots.put(repository, rootAlias);
        }

        // we found a alias but not a repository, so try a scope search
        // class implements \Doctrine\Common\Persistence\ObjectRepository, so search for model name of "repositoryClass"
        if(rootAlias != null && repository == null) {
            MethodReference methodReference = methodReferences.iterator().next();
            PhpClass phpClass = PsiTreeUtil.getParentOfType(methodReference, PhpClass.class);
            if(new Symfony2InterfacesUtil().isInstanceOf(phpClass, "\\Doctrine\\Common\\Persistence\\ObjectRepository")) {
                for(DoctrineModel model: EntityHelper.getModelClasses(project)) {
                    String className = model.getPhpClass().getPresentableFQN();
                    if(className != null) {
                        PhpClass resolvedRepoName = EntityHelper.getEntityRepositoryClass(project, className);
                        if(PhpElementsUtil.isEqualClassName(resolvedRepoName, phpClass.getPresentableFQN())) {
                            roots.put(className, rootAlias);
                            return roots;
                        }
                    }
                }
            }

        }

        return roots;
    }

    public QueryBuilderScopeContext collect() {
        QueryBuilderScopeContext qb = new QueryBuilderScopeContext();

        // doctrine needs valid root with an alias, try to find one in method references or scope
        Map<String, String> map = this.findRootDefinition(methodReferences);
        if(map.size() > 0) {
            Map.Entry<String, String> entry = map.entrySet().iterator().next();
            qb.addTable(entry.getKey(), entry.getValue());
        }

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

            if(collectJoins == true) {
                if(name.equals("join") || name.equals("leftJoin") || name.equals("rightJoin") || name.equals("innerJoin")) {
                    String join = PsiElementUtils.getMethodParameterAt(methodReference, 0);
                    String alias = PsiElementUtils.getMethodParameterAt(methodReference, 1);
                    if(join != null && alias != null) {
                        qb.addJoin(alias, new QueryBuilderJoin(join, alias));
                    }
                }
            }


            if("select".equals(name) || "addSelect".equals(name)) {

                // $qb->select('foo')
                PsiElement psiElement = PsiElementUtils.getMethodParameterPsiElementAt(methodReference, 0);
                String literalValue = PhpElementsUtil.getStringValue(psiElement);
                if(literalValue != null) {
                    qb.addSelect(literalValue);
                }

                // $qb->select(array('foo', 'bar', 'accessoryDetail'))
                if(psiElement instanceof ArrayCreationExpression) {
                    for(PsiElement arrayValue: PsiElementUtils.getChildrenOfTypeAsList(psiElement, PlatformPatterns.psiElement(PhpElementTypes.ARRAY_VALUE))) {
                        if(arrayValue.getChildren().length == 1) {
                            String arrayValueString = PhpElementsUtil.getStringValue(arrayValue.getChildren()[0]);
                            if(arrayValueString != null) {
                                qb.addSelect(arrayValueString);
                            }
                        }
                    }
                }

            }

            // $qb->from('foo', 'select')
            if("from".equals(name)) {
                PsiElement psiElement = PsiElementUtils.getMethodParameterPsiElementAt(methodReference, 1);
                String literalValue = PhpElementsUtil.getStringValue(psiElement);
                if(literalValue != null) {
                    qb.addSelect(literalValue);
                }
            }


        }

        if(qb.getTableMap().size() > 0) {
            Map.Entry<String, String> entry = qb.getTableMap().entrySet().iterator().next();
            String className = entry.getKey();
            PhpClass phpClass = PhpElementsUtil.getClassInterface(project, className);

            // add root select fields
            if(phpClass != null) {
                qb.addPropertyAlias(entry.getValue(), new QueryBuilderPropertyAlias(entry.getValue(), null, phpClass));
                qb.addRelation(entry.getValue(), attachRelationFields(phpClass));
                for(Field field: phpClass.getFields()) {
                    if(!field.isConstant()) {
                        qb.addPropertyAlias(entry.getValue() + "." + field.getName(), new QueryBuilderPropertyAlias(entry.getValue(), field.getName(), field));
                    }
                }
            }

            Resolver resolver = new Resolver(project, entry.getValue(), entry.getKey(), qb.getRelationMap(), qb.getJoinMap());
            resolver.collect();

        }

        if(collectProperties == true) {
            for(QueryBuilderJoin join: qb.getJoinMap().values()) {
                String className = join.getResolvedClass();
                if(className != null) {
                    PhpClass phpClass = PhpElementsUtil.getClassInterface(project, className);
                    if(phpClass != null) {

                        // add main alias;
                        qb.addPropertyAlias(join.getAlias(), new QueryBuilderPropertyAlias(join.getAlias(), null, phpClass));

                        // add entity properties
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

