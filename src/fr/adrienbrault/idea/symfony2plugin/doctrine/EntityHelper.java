package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.PhpNamedElementImpl;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.DocumentNamespacesParser;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.EntityNamesServiceParser;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineTypes;
import fr.adrienbrault.idea.symfony2plugin.extension.DoctrineModelProvider;
import fr.adrienbrault.idea.symfony2plugin.extension.DoctrineModelProviderParameter;
import fr.adrienbrault.idea.symfony2plugin.util.*;
import fr.adrienbrault.idea.symfony2plugin.util.dict.DoctrineModel;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlKeyFinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EntityHelper {

    public static final ExtensionPointName<DoctrineModelProvider> MODEL_POINT_NAME = new ExtensionPointName<DoctrineModelProvider>("fr.adrienbrault.idea.symfony2plugin.extension.DoctrineModelProvider");

    final public static String[] ANNOTATION_FIELDS = new String[] {
        "\\Doctrine\\ORM\\Mapping\\Column",
        "\\Doctrine\\ORM\\Mapping\\OneToOne",
        "\\Doctrine\\ORM\\Mapping\\ManyToOne",
        "\\Doctrine\\ORM\\Mapping\\OneToMany",
        "\\Doctrine\\ORM\\Mapping\\ManyToMany",
    };

    final public static Set<String> RELATIONS = new HashSet<String>(Arrays.asList("manytoone", "manytomany", "onetoone", "onetomany"));

    /**
     * Resolve shortcut and namespaces classes for current phpclass and attached modelname
     */
    public static PhpClass getAnnotationRepositoryClass(PhpClass phpClass, String modelName) {

        // \ns\Class fine we dont need to resolve classname we are in global context
        if(modelName.startsWith("\\")) {
            return PhpElementsUtil.getClassInterface(phpClass.getProject(), modelName);
        }

        // repositoryClass="Classname" pre-append namespace here
        PhpNamedElementImpl phpNamedElementImpl = PsiTreeUtil.getParentOfType(phpClass, PhpNamedElementImpl.class);
        if(phpNamedElementImpl != null) {
            String className = phpNamedElementImpl.getFQN() + "\\" +  modelName;
            PhpClass namespaceClass = PhpElementsUtil.getClassInterface(phpClass.getProject(), className);
            if(namespaceClass != null) {
                return namespaceClass;
            }
        }

        // repositoryClass="Classname\Test" trailing backslash can be stripped
        return  PhpElementsUtil.getClassInterface(phpClass.getProject(), modelName);

    }


    @Nullable
    public static PhpClass getEntityRepositoryClass(Project project, String shortcutName) {

        PhpClass phpClass = resolveShortcutName(project, shortcutName);
        if(phpClass == null) {
            return null;
        }

        // search on annotations
        PhpDocComment docAnnotation = phpClass.getDocComment();
        if(docAnnotation != null) {

            // search for repositoryClass="Foo\Bar\RegisterRepository"
            // @MongoDB\Document; @ORM\Entity
            String docAnnotationText = docAnnotation.getText();
            Matcher matcher = Pattern.compile("repositoryClass[\\s]*=[\\s]*[\"|'](.*)[\"|']").matcher(docAnnotationText);
            if (matcher.find()) {
                return getAnnotationRepositoryClass(phpClass, matcher.group(1));
            }
        }

        SymfonyBundle symfonyBundle = new SymfonyBundleUtil(PhpIndex.getInstance(project)).getContainingBundle(phpClass);
        if(symfonyBundle != null) {
            String classFqnName = phpClass.getPresentableFQN();

            if(classFqnName != null) {
                PhpClass repositoryClass = getEntityRepositoryClass(project, symfonyBundle, classFqnName);
                if(repositoryClass != null) {
                    return repositoryClass;
                }

            }

        }

        // old __CLASS__ Repository type
        // @TODO remove this fallback when we implemented all cases
        return resolveShortcutName(project, shortcutName + "Repository");
    }

    private static List<DoctrineModelField> getModelFieldsSet(YAMLKeyValue yamlKeyValue) {

        List<DoctrineModelField> fields = new ArrayList<DoctrineModelField>();

        for(Map.Entry<String, YAMLKeyValue> entry: getYamlModelFieldKeyValues(yamlKeyValue).entrySet()) {
            List<DoctrineModelField> fieldSet = getYamlDoctrineFields(entry.getKey(), entry.getValue());
            if(fieldSet != null) {
                fields.addAll(fieldSet);
            }
        }

        return fields;
    }


    @Nullable
    public static List<DoctrineModelField> getYamlDoctrineFields(String keyName, @Nullable YAMLKeyValue yamlKeyValue) {

        if(yamlKeyValue == null) {
            return null;
        }

        PsiElement yamlCompoundValue = yamlKeyValue.getValue();
        if(yamlCompoundValue == null) {
            return null;
        }

        List<DoctrineModelField> modelFields = new ArrayList<DoctrineModelField>();
        for(YAMLKeyValue yamlKey: PsiTreeUtil.getChildrenOfTypeAsList(yamlCompoundValue, YAMLKeyValue.class)) {
            String fieldName = YamlHelper.getYamlKeyName(yamlKey);
            if(fieldName != null) {
                DoctrineModelField modelField = new DoctrineModelField(fieldName);
                modelField.addTarget(yamlKey);
                attachYamlFieldTypeName(keyName, modelField, yamlKey);
                modelFields.add(modelField);
            }
        }

        return modelFields;
    }

    public static void attachYamlFieldTypeName(String keyName, DoctrineModelField doctrineModelField, YAMLKeyValue yamlKeyValue) {

        if("fields".equals(keyName) || "id".equals(keyName)) {

            YAMLKeyValue yamlType = YamlHelper.getYamlKeyValue(yamlKeyValue, "type");
            if(yamlType != null && yamlType.getValueText() != null) {
                doctrineModelField.setTypeName(yamlType.getValueText());
            }

            YAMLKeyValue yamlColumn = YamlHelper.getYamlKeyValue(yamlKeyValue, "column");
            if(yamlColumn != null) {
                doctrineModelField.setColumn(yamlColumn.getValueText());
            }

            return;
        }

        if(RELATIONS.contains(keyName.toLowerCase())) {
            YAMLKeyValue targetEntity = YamlHelper.getYamlKeyValue(yamlKeyValue, "targetEntity");
            if(targetEntity != null) {
                doctrineModelField.setRelationType(keyName);
                String value = targetEntity.getValueText();
                if(value != null) {
                    doctrineModelField.setRelation(value);
                }
            }
        }

    }

    @NotNull
    public static Map<String, YAMLKeyValue> getYamlModelFieldKeyValues(YAMLKeyValue yamlKeyValue) {
        Map<String, YAMLKeyValue> keyValueCollection = new HashMap<String, YAMLKeyValue>();

        for(String fieldMap: new String[] { "id", "fields", "manyToOne", "oneToOne", "manyToMany", "oneToMany"}) {
            YAMLKeyValue targetYamlKeyValue = YamlHelper.getYamlKeyValue(yamlKeyValue, fieldMap, true);
            if(targetYamlKeyValue != null) {
                keyValueCollection.put(fieldMap, targetYamlKeyValue);
            }
        }

        return keyValueCollection;
    }

    public static PsiElement[] getModelFieldTargets(PhpClass phpClass, String fieldName) {

        Collection<PsiElement> psiElements = new ArrayList<PsiElement>();

        PsiFile psiFile = EntityHelper.getModelConfigFile(phpClass);

        if(psiFile instanceof YAMLFile) {
            PsiElement yamlDocument = psiFile.getFirstChild();
            if(yamlDocument instanceof YAMLDocument) {
                PsiElement arrayKeyValue = yamlDocument.getFirstChild();
                if(arrayKeyValue instanceof YAMLKeyValue) {
                    for(YAMLKeyValue yamlKeyValue: EntityHelper.getYamlModelFieldKeyValues((YAMLKeyValue) arrayKeyValue).values()) {
                        YAMLKeyValue target = YamlKeyFinder.findKey(yamlKeyValue, fieldName);
                        if(target != null) {
                            psiElements.add(target);
                        }
                    }
                }
            }
        }

        // provide fallback on annotations
        // @TODO: better detect annotation switch; yaml and annotation are valid; need deps on annotation plugin
        PhpDocComment docComment = phpClass.getDocComment();
        if(docComment != null) {
            if(docComment.getText().contains("Entity") || docComment.getText().contains("@ORM") || docComment.getText().contains("repositoryClass")) {
                for(Field field: phpClass.getFields()) {
                    if(!field.isConstant() && fieldName.equals(field.getName())) {
                        psiElements.add(field);
                    }
                }
            }
        }

        String methodName = "get" + StringUtils.camelize(fieldName.toLowerCase(), false);
        Method method = PhpElementsUtil.getClassMethod(phpClass, methodName);
        if(method != null) {
            psiElements.add(method);
        }

        return psiElements.toArray(new PsiElement[psiElements.size()]);

    }

    @Nullable
    public static PsiFile getModelConfigFile(PhpClass phpClass) {

        SymfonyBundle symfonyBundle = new SymfonyBundleUtil(phpClass.getProject()).getContainingBundle(phpClass);
        if(symfonyBundle != null) {
            for(String modelShortcut: new String[] {"orm", "mongodb"}) {
                String fqn = phpClass.getPresentableFQN();

                String className = phpClass.getName();

                if(fqn != null) {
                    int n = fqn.indexOf("\\Entity\\");
                    if(n > 0) {
                        className = fqn.substring(n + 8).replace("\\", ".");
                    }
                }

                String entityFile = "Resources/config/doctrine/" + className + String.format(".%s.yml", modelShortcut);
                VirtualFile virtualFile = symfonyBundle.getRelative(entityFile);
                if(virtualFile != null) {
                    PsiFile psiFile = PsiManager.getInstance(phpClass.getProject()).findFile(virtualFile);
                    if(psiFile != null) {
                        return psiFile;
                    }
                }
            }
        }

        return null;
    }

    public static List<DoctrineModelField> getModelFields(PhpClass phpClass) {

        List<DoctrineModelField> modelFields = new ArrayList<DoctrineModelField>();

        PsiFile psiFile = getModelConfigFile(phpClass);

        if(psiFile instanceof YAMLFile) {
            PsiElement yamlDocument = psiFile.getFirstChild();
            if(yamlDocument instanceof YAMLDocument) {
                PsiElement arrayKeyValue = yamlDocument.getFirstChild();
                if(arrayKeyValue instanceof YAMLKeyValue) {

                    // first line is class name; check of we are right
                    String className = YamlHelper.getYamlKeyName(((YAMLKeyValue) arrayKeyValue));
                    if(PhpElementsUtil.isEqualClassName(phpClass, className)) {
                        modelFields.addAll(getModelFieldsSet((YAMLKeyValue) arrayKeyValue));
                    }

                }
            }

            return modelFields;
        }

        // provide fallback on annotations
        PhpDocComment docComment = phpClass.getDocComment();
        if(docComment != null) {
            if(AnnotationBackportUtil.hasReference(docComment, "\\Doctrine\\ORM\\Mapping\\Entity")) {
                for(Field field: phpClass.getFields()) {
                    if(!field.isConstant()) {
                        if(AnnotationBackportUtil.hasReference(field.getDocComment(), ANNOTATION_FIELDS)) {
                            DoctrineModelField modelField = new DoctrineModelField(field.getName());
                            attachAnnotationInformation(field, modelField.addTarget(field));
                            modelFields.add(modelField);
                        }
                    }
                }
            }
        }

        return modelFields;
    }

    @Nullable
    private static PhpClass getEntityRepositoryClass(Project project, SymfonyBundle symfonyBundle, String classFqnName) {

        // some default bundle search path
        // Bundle/Resources/config/doctrine/Product.orm.yml
        // Bundle/Resources/config/doctrine/Product.mongodb.yml
        List<String[]> managerConfigs = new ArrayList<String[]>();
        managerConfigs.add(new String[] { "Entity", "orm"});
        managerConfigs.add(new String[] { "Document", "mongodb"});

        for(String[] managerConfig: managerConfigs) {
            String entityName = classFqnName.substring(symfonyBundle.getNamespaceName().length() - 1);
            if(entityName.startsWith(managerConfig[0] + "\\")) {
                entityName =  entityName.substring((managerConfig[0] + "\\").length());
            }

            // entities in sub folder: 'Foo\Bar' -> 'Foo.Bar.orm.yml'
            String entityFile = "Resources/config/doctrine/" + entityName.replace("\\", ".") + String.format(".%s.yml", managerConfig[1]);
            VirtualFile virtualFile = symfonyBundle.getRelative(entityFile);
            if(virtualFile != null) {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                if(psiFile != null) {

                    // search for "repositoryClass: Foo\Bar\RegisterRepository" also provide quoted values
                    Matcher matcher = Pattern.compile("[\\s]*repositoryClass:[\\s]*[\"|']*(.*)[\"|']*").matcher(psiFile.getText());
                    if (matcher.find()) {
                        return PhpElementsUtil.getClass(PhpIndex.getInstance(project), matcher.group(1));
                    }

                    // we found entity config so no other check needed
                    return null;
                }

            }
        }

        return null;
    }

    @Nullable
    public static PhpClass resolveShortcutName(Project project, String shortcutName) {
        return resolveShortcutName(project, shortcutName, DoctrineTypes.Manager.ORM, DoctrineTypes.Manager.MONGO_DB);
    }

    /**
     *
     * @param project PHPStorm projects
     * @param shortcutName name as MyBundle\Entity\Model or MyBundle:Model
     * @return null|PhpClass
     */
    @Nullable
    public static PhpClass resolveShortcutName(Project project, String shortcutName, DoctrineTypes.Manager... managers) {

        List<DoctrineTypes.Manager> managerList = Arrays.asList(managers);

        if(shortcutName == null) {
            return null;
        }

        String entity_name = shortcutName;

        // resolve:
        // MyBundle:Model -> MyBundle\Entity\Model
        // MyBundle:Folder\Model -> MyBundle\Entity\Folder\Model
        if (shortcutName.contains(":")) {

            Map<String, String> em = new HashMap<String, String>();

            if(managerList.contains(DoctrineTypes.Manager.ORM)) {
                em.putAll(ServiceXmlParserFactory.getInstance(project, EntityNamesServiceParser.class).getEntityNameMap());
            }

            if(managerList.contains(DoctrineTypes.Manager.MONGO_DB)) {
                em.putAll(ServiceXmlParserFactory.getInstance(project, DocumentNamespacesParser.class).getNamespaceMap());
            }

            int firstDirectorySeparatorIndex = shortcutName.indexOf(":");

            String bundlename = shortcutName.substring(0, firstDirectorySeparatorIndex);
            String entityName = shortcutName.substring(firstDirectorySeparatorIndex + 1);

            String namespace = em.get(bundlename);

            if(namespace == null) {
                return null;
            }

            entity_name = namespace + "\\" + entityName;
        }

        // only use them on entity namespace
        if(!entity_name.contains("\\")) {
            return null;
        }

        // dont we have any unique class getting method here?
        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<PhpClass> entity_classes = phpIndex.getClassesByFQN(entity_name);
        if(!entity_classes.isEmpty()){
            return entity_classes.iterator().next();
        }

        return null;
    }

    @Nullable
    public static DoctrineTypes.Manager getManager(MethodReference methodReference) {

        PhpPsiElement phpTypedElement = methodReference.getFirstPsiChild();
        if(!(phpTypedElement instanceof PhpTypedElement)) {
            return null;
        }

        Symfony2InterfacesUtil symfony2InterfacesUtil = new Symfony2InterfacesUtil();
        for(String typeString: PhpIndex.getInstance(methodReference.getProject()).completeType(methodReference.getProject(), ((PhpTypedElement) phpTypedElement).getType(), new HashSet<String>()).getTypes()) {
            for(Map.Entry<DoctrineTypes.Manager, String> entry: DoctrineTypes.getManagerInstanceMap().entrySet()) {
                if(symfony2InterfacesUtil.isInstanceOf(methodReference.getProject(), typeString, entry.getValue())) {
                    return entry.getKey();
                }
            }
        }

        return null;
    }

    public static PsiElement[] getModelPsiTargets(Project project, @NotNull String entityName) {
        List<PsiElement> results = new ArrayList<PsiElement>();

        PhpClass phpClass = EntityHelper.getEntityRepositoryClass(project, entityName);
        if(phpClass != null) {
            results.add(phpClass);
        }

        // search any php model file
        PhpClass entity = EntityHelper.resolveShortcutName(project, entityName);
        if(entity != null) {
            results.add(entity);

            // find model config eg ClassName.orm.yml
            PsiFile psiFile = EntityHelper.getModelConfigFile(entity);
            if(psiFile != null) {
                results.add(psiFile);
            }

        }

        return results.toArray(new PsiElement[results.size()]);
    }

    public static void attachAnnotationInformation(Field field, DoctrineModelField doctrineModelField) {

        // we already have that without regular expression
        // @TODO: de.espend.idea.php.annotation.util.AnnotationUtil.getPhpDocCommentAnnotationContainer()
        // fully require plugin now?

        // get some more presentable completion information
        // dont resolve docblocks; just extract them from doc comment
        PhpDocComment docBlock = field.getDocComment();

        if(docBlock == null) {
            return;
        }

        String text = docBlock.getText();

        // column type
        Matcher matcher = Pattern.compile("type[\\s]*=[\\s]*[\"|']([\\w_\\\\]+)[\"|']").matcher(text);
        if (matcher.find()) {
            doctrineModelField.setTypeName(matcher.group(1));
        }

        // targetEntity name
        matcher = Pattern.compile("targetEntity[\\s]*=[\\s]*[\"|']([\\w_\\\\]+)[\"|']").matcher(text);
        if (matcher.find()) {
            doctrineModelField.setRelation(matcher.group(1));
        }

        // relation type
        matcher = Pattern.compile("((Many|One)To(Many|One))\\(").matcher(text);
        if (matcher.find()) {
            doctrineModelField.setRelationType(matcher.group(1));
        }

        matcher = Pattern.compile("Column\\(").matcher(text);
        if (matcher.find()) {
            matcher = Pattern.compile("name\\s*=\\s*\"(\\w+)\"").matcher(text);
            if(matcher.find()) {
                doctrineModelField.setColumn(matcher.group(1));
            }

        }

    }

    public static Collection<DoctrineModel> getModelClasses(final Project project) {

        Collection<DoctrineModel> doctrineModels = getModelClasses(project, new HashMap<String, String>() {{
            putAll(ServiceXmlParserFactory.getInstance(project, EntityNamesServiceParser.class).getEntityNameMap());
            putAll(ServiceXmlParserFactory.getInstance(project, DocumentNamespacesParser.class).getNamespaceMap());
        }});


        DoctrineModelProviderParameter containerLoaderExtensionParameter = new DoctrineModelProviderParameter(project, new ArrayList<DoctrineModelProviderParameter.DoctrineModel>());
        for(DoctrineModelProvider provider : EntityHelper.MODEL_POINT_NAME.getExtensions()) {
            for(DoctrineModelProviderParameter.DoctrineModel doctrineModel: provider.collectModels(containerLoaderExtensionParameter)) {
                doctrineModels.add(new DoctrineModel(doctrineModel.getPhpClass(), doctrineModel.getName()));
            }
        }

        return doctrineModels;
    }

    public static Collection<DoctrineModel> getModelClasses(Project project, Map<String, String> shortcutNames) {

        Collection<DoctrineModel> models = new ArrayList<DoctrineModel>();

        PhpClass repositoryInterface = PhpElementsUtil.getInterface(PhpIndex.getInstance(project), DoctrineTypes.REPOSITORY_INTERFACE);
        if(null == repositoryInterface) {
            return models;
        }

        for (Map.Entry<String, String> entry : shortcutNames.entrySet()) {

            Collection<PhpClass> phpClasses = PhpIndexUtil.getPhpClassInsideNamespace(repositoryInterface.getProject(), entry.getValue());
            for(PhpClass phpClass: phpClasses) {
                if(isEntity(phpClass, repositoryInterface)) {
                    models.add(new DoctrineModel(phpClass, entry.getKey(), entry.getValue()));
                }
            }

        }

        return models;
    }

    public static boolean isEntity(PhpClass entityClass, PhpClass repositoryClass) {

        if(entityClass.isAbstract()) {
            return false;
        }

        Symfony2InterfacesUtil symfony2Util = new Symfony2InterfacesUtil();
        return !symfony2Util.isInstanceOf(entityClass, repositoryClass);
    }


}
