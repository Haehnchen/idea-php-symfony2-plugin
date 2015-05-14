package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder;

import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.ObjectRepositoryTypeProvider;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.detector.FormQueryBuilderRepositoryDetector;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.detector.QueryBuilderRepositoryDetector;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.detector.QueryBuilderRepositoryDetectorParameter;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict.QueryBuilderJoin;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict.QueryBuilderPropertyAlias;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict.QueryBuilderRelation;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.DoctrineModel;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryBuilderMethodReferenceParser {

    private boolean collectParameter = true;
    private boolean collectJoins = true;
    private boolean collectProperties = true;

    final private Project project;
    final private Collection<MethodReference> methodReferences;
    
    private static QueryBuilderRepositoryDetector[] repositoryDetectors = new QueryBuilderRepositoryDetector[] {
        new FormQueryBuilderRepositoryDetector(),
    };

    public QueryBuilderMethodReferenceParser(Project project, Collection<MethodReference> methodReferences) {
        this.methodReferences = methodReferences;
        this.project = project;
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
            if(name != null) {
                collectParameter(qb, methodReference, name);
                collectJoins(qb, methodReference, name);
                collectSelects(qb, methodReference, name);
                collectSelectInForm(qb, methodReference, name);
            }

        }

        // first tableMap entry is root, we add several initial data
        if(qb.getTableMap().size() > 0) {
            Map.Entry<String, String> entry = qb.getTableMap().entrySet().iterator().next();
            String className = entry.getKey();
            PhpClass phpClass = PhpElementsUtil.getClassInterface(project, className);

            // add root select fields
            if(phpClass != null) {

                qb.addPropertyAlias(entry.getValue(), new QueryBuilderPropertyAlias(entry.getValue(), null, new DoctrineModelField(entry.getValue()).addTarget(phpClass).setTypeName(phpClass.getPresentableFQN())));

                List<QueryBuilderRelation> relationList = new ArrayList<QueryBuilderRelation>();

                //qb.addRelation(entry.getValue(), attachRelationFields(phpClass));
                for(DoctrineModelField field: EntityHelper.getModelFields(phpClass)) {
                    qb.addPropertyAlias(entry.getValue() + "." + field.getName(), new QueryBuilderPropertyAlias(entry.getValue(), field.getName(), field));
                    if(field.getRelation() != null && field.getRelationType() != null) {
                        relationList.add(new QueryBuilderRelation(field.getName(), field.getRelation()));
                    }
                }

                qb.addRelation(entry.getValue(), relationList);
            }

            QueryBuilderRelationClassResolver resolver = new QueryBuilderRelationClassResolver(project, entry.getValue(), entry.getKey(), qb.getRelationMap(), qb.getJoinMap());
            resolver.collect();

        }

        // we have a querybuilder which complete known elements now
        // se we can builder a property (field) map table from it
        this.buildPropertyMap(qb);

        return qb;

    }

    private void collectSelectInForm(QueryBuilderScopeContext qb, MethodReference methodReference, String name) {

        // $qb->from('foo', 'select')
        if(!"from".equals(name)) {
            return;
        }

        PsiElement psiElement = PsiElementUtils.getMethodParameterPsiElementAt(methodReference, 1);
        String literalValue = PhpElementsUtil.getStringValue(psiElement);
        if(literalValue != null) {
            qb.addSelect(literalValue);
        }

    }

    private void collectSelects(QueryBuilderScopeContext qb, MethodReference methodReference, String name) {

        if(!Arrays.asList("select", "addSelect").contains(name)) {
            return;
        }

        // $qb->select('foo')
        // $qb->select('foo', 'foo1')
        for(PsiElement parameter: methodReference.getParameters()) {
            String literalValue = PhpElementsUtil.getStringValue(parameter);
            if(literalValue != null) {
                qb.addSelect(literalValue);
            }
        }

        // $qb->select(array('foo', 'bar', 'accessoryDetail'))
        PsiElement psiElement = PsiElementUtils.getMethodParameterPsiElementAt(methodReference, 0);
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

    private void collectJoins(QueryBuilderScopeContext qb, MethodReference methodReference, String name) {

        if(!collectJoins || !Arrays.asList("join", "leftJoin", "rightJoin", "innerJoin").contains(name)) {
            return;
        }

        String join = PsiElementUtils.getMethodParameterAt(methodReference, 0);
        String alias = PsiElementUtils.getMethodParameterAt(methodReference, 1);
        if(join != null && alias != null) {
            qb.addJoin(alias, new QueryBuilderJoin(join, alias));
        }

    }

    private void collectParameter(QueryBuilderScopeContext qb, MethodReference methodReference, String name) {

        if(!collectParameter || !Arrays.asList("where", "andWhere").contains(name)) {
            return;
        }

        String value = PsiElementUtils.getMethodParameterAt(methodReference, 0);
        if(value != null) {
            Matcher matcher = Pattern.compile(":(\\w+)", Pattern.MULTILINE).matcher(value);
            while(matcher.find()){
                qb.addParameter(matcher.group(1));
            }
        }

    }


    private Map<String, String> findRootDefinition(Collection<MethodReference> methodReferences) {

        Map<String, String> roots = new HashMap<String, String>();

        if(methodReferences.size() == 0) {
            return roots;
        }

        String rootAlias = null;
        String repository = null;


        for(MethodReference methodReference: methodReferences) {
            String methodReferenceName = methodReference.getName();

            // get alias
            // ->createQueryBuilder('test');
            if("createQueryBuilder".equals(methodReferenceName)) {
                String possibleAlias = PhpElementsUtil.getStringValue(PsiElementUtils.getMethodParameterPsiElementAt(methodReference, 0));
                if(possibleAlias != null) {
                    rootAlias = possibleAlias;
                }
            }

            // find repository class
            // getRepository('Foo')->createQueryBuilder('test');
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
                    PhpClass phpClass = EntityHelper.resolveShortcutName(project, table);
                    if(phpClass != null) {
                        table = phpClass.getPresentableFQN();
                    }

                    roots.put(table, alias);
                }
            }

        }

        // we have a valid root so add it
        if(rootAlias != null && repository != null) {
            roots.put(repository, rootAlias);
        }

        // we found a alias but not a repository name, so try a scope search if we are inside repository class
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

        // search on PhpTypeProvider
        // $er->createQueryBuilder()
        if(rootAlias != null && repository == null) {
            for(MethodReference methodReference: methodReferences) {
                if("createQueryBuilder".equals(methodReference.getName())) {
                    String signature = methodReference.getSignature();
                    int endIndex = signature.lastIndexOf(ObjectRepositoryTypeProvider.TRIM_KEY);
                    if(endIndex != -1) {
                        String parameter = signature.substring(endIndex + 1);
                        int point = parameter.indexOf(".");
                        if(point > -1) {
                            parameter = parameter.substring(0, point);
                            parameter = PhpTypeProviderUtil.getResolvedParameter(PhpIndex.getInstance(project), parameter);
                            if(parameter != null) {
                                PhpClass phpClass = EntityHelper.resolveShortcutName(project, parameter);
                                if(phpClass != null && phpClass.getPresentableFQN() != null) {
                                    roots.put(phpClass.getPresentableFQN(), rootAlias);
                                    return roots;
                                }
                            }
                        }
                    }
                }
            }
        }

        if(rootAlias != null && repository == null) {
            for (QueryBuilderRepositoryDetector detector: repositoryDetectors) {
                String result = detector.getRepository(new QueryBuilderRepositoryDetectorParameter(project, methodReferences));
                if(result != null) {
                    roots.put(result, rootAlias);
                    return roots;
                }
            }
        }

        return roots;
    }

    private void buildPropertyMap(QueryBuilderScopeContext qb) {

        if(!collectProperties) {
            return;
        }

        for(QueryBuilderJoin join: qb.getJoinMap().values()) {
            String className = join.getResolvedClass();
            if(className != null) {
                PhpClass phpClass = PhpElementsUtil.getClassInterface(project, className);
                if(phpClass != null) {
                    qb.addPropertyAlias(join.getAlias(), new QueryBuilderPropertyAlias(join.getAlias(), null, new DoctrineModelField(join.getAlias()).addTarget(phpClass).setTypeName(phpClass.getPresentableFQN())));

                    // add entity properties
                    for(DoctrineModelField field: EntityHelper.getModelFields(phpClass)) {
                        qb.addPropertyAlias(join.getAlias() + "." + field.getName(), new QueryBuilderPropertyAlias(join.getAlias(), field.getName(), field));
                    }
                }
            }
        }

    }

    public static List<QueryBuilderRelation> attachRelationFields(PhpClass phpClass) {

        List<QueryBuilderRelation> relations = new ArrayList<QueryBuilderRelation>();

        for(DoctrineModelField field: EntityHelper.getModelFields(phpClass)) {
            if(field.getRelation() != null && field.getRelationType() != null) {
                relations.add(new QueryBuilderRelation(field.getName(), field.getRelation()));
            }
        }

        return relations;
    }

}

