package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder;

import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.detector.FormQueryBuilderRepositoryDetector;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.detector.QueryBuilderRepositoryDetector;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.detector.QueryBuilderRepositoryDetectorParameter;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict.QueryBuilderClassJoin;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict.QueryBuilderJoin;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict.QueryBuilderPropertyAlias;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict.QueryBuilderRelation;
import fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.util.QueryBuilderUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.DoctrineModel;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
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

                List<QueryBuilderRelation> relationList = new ArrayList<>();

                //qb.addRelation(entry.getValue(), attachRelationFields(phpClass));
                for(DoctrineModelField field: EntityHelper.getModelFields(phpClass)) {
                    qb.addPropertyAlias(entry.getValue() + "." + field.getName(), new QueryBuilderPropertyAlias(entry.getValue(), field.getName(), field));
                    if(field.getRelation() != null && field.getRelationType() != null) {
                        relationList.add(new QueryBuilderRelation(field.getName(), field.getRelation()));
                    }
                }

                qb.addRelation(entry.getValue(), relationList);
            }

            QueryBuilderRelationClassResolver resolver = new QueryBuilderRelationClassResolver(project, entry.getValue(), entry.getKey(), qb.getRelationMap(), qb.getJoinMap(), qb.getJoinClassMap());
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

        PsiElement parameter = methodReference.getParameter(0);
        if (parameter instanceof ClassConstantReference classConstantReference) {
            // Foobar\Foobar::class
            String alias = PsiElementUtils.getMethodParameterAt(methodReference, 1);

            if (StringUtils.isNotBlank(alias) && classConstantReference.getClassReference() instanceof ClassReference classReference) {
                String fqn = classReference.getFQN();
                if (fqn != null) {
                    qb.addClassJoin(alias, new QueryBuilderClassJoin(fqn, alias));
                }
            }
        } else {
            String join = PsiElementUtils.getMethodParameterAt(methodReference, 0);
            String alias = PsiElementUtils.getMethodParameterAt(methodReference, 1);

            if (join != null && alias != null) {
                if (!join.contains(".") && join.contains("\\")) {
                    // Foobar\Foobar
                    // \Foobar\Foobar
                    qb.addClassJoin(alias, new QueryBuilderClassJoin("\\" + StringUtils.stripStart(join, "\\"), alias));
                } else {
                    // foo.bar
                    qb.addJoin(alias, new QueryBuilderJoin(join, alias));
                }
            }
        }
    }

    private void collectParameter(QueryBuilderScopeContext qb, MethodReference methodReference, String name) {
        if (!collectParameter || !Arrays.asList("where", "andWhere").contains(name)) {
            return;
        }

        for (PsiElement parameter : methodReference.getParameters()) {
            String value = PsiElementUtils.getMethodParameter(parameter);
            if (value == null) {
                continue;
            }

            Matcher matcher = Pattern.compile(":(\\w+)", Pattern.MULTILINE).matcher(value);
            while (matcher.find()){
                qb.addParameter(matcher.group(1));
            }
        }
    }

    /**
     * Extract model from context
     *
     * ```
     * getRepository('Foo')->createQueryBuilder('test')
     * ```
     *
     * ```
     * public function __construct(RegistryInterface $registry)
     * {
     *  parent::__construct($registry, Entity::class);
     * }
     * ```
     */
    @NotNull
    private Map<String, String> findRootDefinition(@NotNull Collection<MethodReference> methodReferences) {
        if(methodReferences.size() == 0) {
            return Collections.emptyMap();
        }

        Map<String, String> roots = new HashMap<>();
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

            // $qb->from(Class::Class, 'article')
            // $qb->from('Foo\Class', 'article')
            if("from".equals(methodReferenceName)) {
                PsiElement typeParameter = PsiElementUtils.getMethodParameterPsiElementAt(methodReference, 0);

                // class constant condition
                String table;
                if(typeParameter instanceof ClassConstantReference) {
                    table = PhpElementsUtil.getClassConstantPhpFqn((ClassConstantReference) typeParameter);
                } else {
                    table = PhpElementsUtil.getStringValue(typeParameter);
                }

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

        // public function __construct(RegistryInterface $registry)
        //   parent::__construct($registry, Entity::class);
        if(rootAlias != null && repository == null) {
            MethodReference methodReference = methodReferences.iterator().next();
            PhpClass phpClass = PsiTreeUtil.getParentOfType(methodReference, PhpClass.class);

            if(phpClass != null && PhpElementsUtil.isInstanceOf(phpClass, "\\Doctrine\\Bundle\\DoctrineBundle\\Repository\\ServiceEntityRepository")) {
                Method constructor = phpClass.getConstructor();
                if (constructor != null) {
                    for (MethodReference reference : PsiTreeUtil.findChildrenOfType(constructor, MethodReference.class)) {
                        if ("__construct".equals(reference.getName())) {
                            PsiElement[] parameters = reference.getParameters();
                            if (parameters.length > 1) {
                                PsiElement parameter = parameters[1];
                                String stringValue = PhpElementsUtil.getStringValue(parameter);
                                if (stringValue != null) {
                                    roots.put(stringValue, rootAlias);
                                    return roots;
                                }
                            }
                        }
                    }
                }
            }
        }

        // we found a alias but not a repository name, so try a scope search if we are inside repository class
        // class implements \Doctrine\Common\Persistence\ObjectRepository, so search for model name of "repositoryClass"
        if(rootAlias != null && repository == null) {
            MethodReference methodReference = methodReferences.iterator().next();
            PhpClass phpClass = PsiTreeUtil.getParentOfType(methodReference, PhpClass.class);

            if(
                phpClass != null &&
                (
                    PhpElementsUtil.isInstanceOf(phpClass, "\\Doctrine\\Common\\Persistence\\ObjectRepository") ||
                    PhpElementsUtil.isInstanceOf(phpClass, "\\Doctrine\\Persistence\\ObjectRepository")
                )
            ) {
                for(DoctrineModel model: EntityHelper.getModelClasses(project)) {
                    String className = model.getPhpClass().getPresentableFQN();
                    PhpClass resolvedRepoName = EntityHelper.getEntityRepositoryClass(project, className);
                    if(PhpElementsUtil.isEqualClassName(resolvedRepoName, phpClass.getPresentableFQN())) {
                        roots.put(className, rootAlias);
                        return roots;
                    }
                }
            }
        }

        // search on PhpTypeProvider
        // $er->createQueryBuilder()
        if(rootAlias != null && repository == null) {
            for(MethodReference methodReference: methodReferences) {
                if("createQueryBuilder".equals(methodReference.getName())) {
                    String signatures = methodReference.getSignature();
                    for (String signature : QueryBuilderUtil.extractQueryBuilderRepositoryParameters(signatures)) {
                        // Resolve: "espend...ModelBundle:Car", "#K#C\...\Entity\Car.class", ...
                        String parameter = PhpTypeProviderUtil.getResolvedParameter(PhpIndex.getInstance(project), signature);
                        if(parameter != null) {
                            PhpClass phpClass = EntityHelper.resolveShortcutName(project, parameter);
                            if(phpClass != null) {
                                roots.put(phpClass.getPresentableFQN(), rootAlias);
                                return roots;
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
            if (className != null) {
                PhpClass phpClass = PhpElementsUtil.getClassInterface(project, className);
                if (phpClass != null) {
                    qb.addPropertyAlias(join.getAlias(), new QueryBuilderPropertyAlias(join.getAlias(), null, new DoctrineModelField(join.getAlias()).addTarget(phpClass).setTypeName(phpClass.getPresentableFQN())));

                    // add entity properties
                    for(DoctrineModelField field: EntityHelper.getModelFields(phpClass)) {
                        qb.addPropertyAlias(join.getAlias() + "." + field.getName(), new QueryBuilderPropertyAlias(join.getAlias(), field.getName(), field));
                    }
                }
            }
        }

        for(QueryBuilderClassJoin join: qb.getJoinClassMap().values()) {
            PhpClass phpClass = PhpElementsUtil.getClassInterface(project, join.className());
            if (phpClass != null) {
                qb.addPropertyAlias(join.alias(), new QueryBuilderPropertyAlias(join.alias(), null, new DoctrineModelField(join.alias()).addTarget(phpClass).setTypeName(phpClass.getPresentableFQN())));

                // add entity properties
                for(DoctrineModelField field: EntityHelper.getModelFields(phpClass)) {
                    qb.addPropertyAlias(join.alias() + "." + field.getName(), new QueryBuilderPropertyAlias(join.alias(), field.getName(), field));
                }
            }
        }
    }

    public static List<QueryBuilderRelation> attachRelationFields(PhpClass phpClass) {

        List<QueryBuilderRelation> relations = new ArrayList<>();

        for(DoctrineModelField field: EntityHelper.getModelFields(phpClass)) {
            if(field.getRelation() != null && field.getRelationType() != null) {
                relations.add(new QueryBuilderRelation(field.getName(), field.getRelation()));
            }
        }

        return relations;
    }

}

