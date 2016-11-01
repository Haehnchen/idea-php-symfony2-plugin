package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocParamTag;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.PhpNamedElementImpl;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.DocumentNamespacesParser;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.EntityNamesServiceParser;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineTypes;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
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
import org.jetbrains.yaml.psi.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EntityHelper {

    public static final ExtensionPointName<DoctrineModelProvider> MODEL_POINT_NAME = new ExtensionPointName<>("fr.adrienbrault.idea.symfony2plugin.extension.DoctrineModelProvider");

    final public static String[] ANNOTATION_FIELDS = new String[] {
        "\\Doctrine\\ORM\\Mapping\\Column",
        "\\Doctrine\\ORM\\Mapping\\OneToOne",
        "\\Doctrine\\ORM\\Mapping\\ManyToOne",
        "\\Doctrine\\ORM\\Mapping\\OneToMany",
        "\\Doctrine\\ORM\\Mapping\\ManyToMany",
    };

    final public static Set<String> RELATIONS = new HashSet<>(Arrays.asList("manytoone", "manytomany", "onetoone", "onetomany"));

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


    /**
     * Search for a repository class of a model
     *
     * @param project Current project
     * @param shortcutName "\Class\Name" or "FooBundle:Name"
     */
    @Nullable
    public static PhpClass getEntityRepositoryClass(@NotNull Project project, @NotNull String shortcutName) {

        PhpClass phpClass = resolveShortcutName(project, shortcutName);
        if(phpClass == null) {
            return null;
        }

        String presentableFQN = phpClass.getPresentableFQN();
        PhpClass classRepository = DoctrineMetadataUtil.getClassRepository(project, presentableFQN);
        if(classRepository != null) {
            return classRepository;
        }

        // @TODO: deprecated code
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
            PhpClass repositoryClass = getEntityRepositoryClass(project, symfonyBundle, presentableFQN);
            if(repositoryClass != null) {
                return repositoryClass;
            }
        }

        // old __CLASS__ Repository type
        // @TODO remove this fallback when we implemented all cases
        return resolveShortcutName(project, shortcutName + "Repository");
    }

    public static List<DoctrineModelField> getModelFieldsSet(YAMLKeyValue yamlKeyValue) {

        List<DoctrineModelField> fields = new ArrayList<>();

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

        List<DoctrineModelField> modelFields = new ArrayList<>();
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
            if(yamlType != null) {
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
                doctrineModelField.setRelation(getOrmClass(yamlKeyValue.getContainingFile(), targetEntity.getValueText()));
            }
        }

    }

    @NotNull
    public static String getOrmClass(@NotNull PsiFile psiFile, @NotNull String className) {

        // force global namespace not need to search for class
        if(className.startsWith("\\")) {
            return className;
        }

        String entityName = null;

        // espend\Doctrine\ModelBundle\Entity\Bike:
        // ...
        // targetEntity: Foo
        if(psiFile instanceof YAMLFile) {
            YAMLDocument yamlDocument = PsiTreeUtil.getChildOfType(psiFile, YAMLDocument.class);
            if(yamlDocument != null) {
                YAMLKeyValue entityKeyValue = PsiTreeUtil.getChildOfType(yamlDocument, YAMLKeyValue.class);
                if(entityKeyValue != null) {
                    entityName = entityKeyValue.getKeyText();
                }
            }
        } else if(psiFile instanceof XmlFile) {

            XmlTag rootTag = ((XmlFile) psiFile).getRootTag();
            if(rootTag != null) {
                XmlTag entity = rootTag.findFirstSubTag("entity");
                if(entity != null) {
                    String name = entity.getAttributeValue("name");
                    if(org.apache.commons.lang.StringUtils.isBlank(name)) {
                        entityName = name;
                    }
                }
            }
        }

        if(entityName == null) {
            return className;
        }

        // trim class name
        int lastBackSlash = entityName.lastIndexOf("\\");
        if(lastBackSlash > 0) {
            String fqnClass = entityName.substring(0, lastBackSlash + 1) + className;
            if(PhpElementsUtil.getClass(psiFile.getProject(), fqnClass) != null) {
                return fqnClass;
            }
        }

        return className;
    }

    @NotNull
    public static Map<String, YAMLKeyValue> getYamlModelFieldKeyValues(YAMLKeyValue yamlKeyValue) {
        Map<String, YAMLKeyValue> keyValueCollection = new HashMap<>();

        for(String fieldMap: new String[] { "id", "fields", "manyToOne", "oneToOne", "manyToMany", "oneToMany"}) {
            YAMLKeyValue targetYamlKeyValue = YamlHelper.getYamlKeyValue(yamlKeyValue, fieldMap, true);
            if(targetYamlKeyValue != null) {
                keyValueCollection.put(fieldMap, targetYamlKeyValue);
            }
        }

        return keyValueCollection;
    }

    @NotNull
    public static PsiElement[] getModelFieldTargets(@NotNull PhpClass phpClass,@NotNull String fieldName) {

        Collection<PsiElement> psiElements = new ArrayList<>();

        DoctrineMetadataModel modelFields = DoctrineMetadataUtil.getModelFields(phpClass.getProject(), phpClass.getPresentableFQN());
        if(modelFields != null) {
            for (DoctrineModelField field : modelFields.getFields()) {
                if(field.getName().equals(fieldName) && field.getTargets().size() > 0) {
                    return field.getTargets().toArray(new PsiElement[psiElements.size()]);
                }
            }
        }

        // @TODO: deprecated
        PsiFile psiFile = EntityHelper.getModelConfigFile(phpClass);

        if(psiFile instanceof YAMLFile) {
            // @TODO: migrate to getEntityFields()
            YAMLValue topLevelValue = ((YAMLFile) psiFile).getDocuments().get(0).getTopLevelValue();
            if(topLevelValue instanceof YAMLMapping) {
                Collection<YAMLKeyValue> keyValues = ((YAMLMapping) topLevelValue).getKeyValues();
                if(keyValues.size() > 0) {
                    for(YAMLKeyValue yamlKeyValue: EntityHelper.getYamlModelFieldKeyValues(keyValues.iterator().next()).values()) {
                        ContainerUtil.addIfNotNull(psiElements, YamlHelper.getYamlKeyValue(yamlKeyValue, "name"));
                    }
                }
            }
        }

        if(psiFile instanceof XmlFile) {
            for (DoctrineModelField field : getEntityFields((XmlFile) psiFile)) {
                if(field.getName().equals(fieldName)) {
                    psiElements.addAll(field.getTargets());
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
        Method method = phpClass.findMethodByName(methodName);
        if(method != null) {
            psiElements.add(method);
        }

        return psiElements.toArray(new PsiElement[psiElements.size()]);

    }

    @Nullable
    private static PsiFile getEntityMetadataFile(@NotNull Project project, @NotNull SymfonyBundle symfonyBundleUtil, @NotNull String className, @NotNull String modelShortcut) {

        for(String s: new String[] {"yml", "xml"}) {

            String entityFile = "Resources/config/doctrine/" + className + String.format(".%s.%s", modelShortcut, s);
            VirtualFile virtualFile = symfonyBundleUtil.getRelative(entityFile);
            if(virtualFile != null) {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                if(psiFile != null) {
                    return psiFile;
                }
            }

        }

        return null;
    }

    @Nullable
    public static PsiFile getModelConfigFile(@NotNull PhpClass phpClass) {

        // new code
        String presentableFQN = phpClass.getPresentableFQN();
        Collection<VirtualFile> metadataFiles = DoctrineMetadataUtil.findMetadataFiles(phpClass.getProject(), presentableFQN);
        if(metadataFiles.size() > 0) {
            PsiFile file = PsiManager.getInstance(phpClass.getProject()).findFile(metadataFiles.iterator().next());
            if(file != null) {
                return file;
            }
        }

        // @TODO: deprecated code
        SymfonyBundle symfonyBundle = new SymfonyBundleUtil(phpClass.getProject()).getContainingBundle(phpClass);
        if(symfonyBundle != null) {
            for(String modelShortcut: new String[] {"orm", "mongodb", "couchdb"}) {
                String className = phpClass.getName();

                int n = presentableFQN.indexOf("\\Entity\\");
                if(n > 0) {
                    className = presentableFQN.substring(n + 8).replace("\\", ".");
                }

                PsiFile entityMetadataFile = getEntityMetadataFile(phpClass.getProject(), symfonyBundle, className, modelShortcut);
                if(entityMetadataFile != null) {
                    return entityMetadataFile;
                }

            }
        }

        return null;
    }

    @NotNull
    public static Collection<DoctrineModelField> getModelFields(@NotNull PhpClass phpClass) {

        // new code
        String presentableFQN = phpClass.getPresentableFQN();
        DoctrineMetadataModel fields = DoctrineMetadataUtil.getModelFields(phpClass.getProject(), presentableFQN);
        if(fields != null) {
            return fields.getFields();
        }

        // @TODO: old deprecated code
        PsiFile psiFile = getModelConfigFile(phpClass);
        if(psiFile == null) {
            Collections.emptyList();
        }

        if(psiFile instanceof YAMLFile) {
            List<DoctrineModelField> modelFields = new ArrayList<>();

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

        if(psiFile instanceof XmlFile) {
            return getEntityFields((XmlFile) psiFile);
        }

        // provide fallback on annotations
        List<DoctrineModelField> modelFields = new ArrayList<>();

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

    @NotNull
    public static List<DoctrineModelField> getEntityFields(@NotNull XmlFile psiFile) {

        List<DoctrineModelField> modelFields = new ArrayList<>();

        XmlTag rootTag = psiFile.getRootTag();
        if(rootTag == null) {
            return Collections.emptyList();
        }

        final XmlTag entity = rootTag.findFirstSubTag("entity");
        if(entity == null) {
            return Collections.emptyList();
        }

        for (XmlTag xmlTag : new ArrayList<XmlTag>() {{
            addAll(Arrays.asList(entity.findSubTags("field")));
            addAll(Arrays.asList(entity.findSubTags("id")));
        }}) {

            String name = xmlTag.getAttributeValue("name");
            if(org.apache.commons.lang.StringUtils.isBlank(name)) {
                continue;
            }

            DoctrineModelField field = new DoctrineModelField(name);

            field.addTarget(xmlTag);

            String column = xmlTag.getAttributeValue("column");
            if(org.apache.commons.lang.StringUtils.isNotBlank(name)) {
                field.setColumn(column);
            }

            String type = xmlTag.getAttributeValue("type");
            if(org.apache.commons.lang.StringUtils.isNotBlank(type)) {
                field.setTypeName(type);
            }

            modelFields.add(field);
        }

        for(String s: new String[] {"one-to-one", "one-to-many", "many-to-many", "many-to-one"}) {
            for (XmlTag xmlTag : entity.findSubTags(s)) {

                String targetEntity = xmlTag.getAttributeValue("target-entity");
                if(targetEntity == null) {
                    continue;
                }

                String field = xmlTag.getAttributeValue("field");
                if(field == null) {
                    continue;
                }

                DoctrineModelField entityField = new DoctrineModelField(field);
                entityField.addTarget(xmlTag);

                // find namespace
                entityField.setRelation(getOrmClass(psiFile, targetEntity));

                entityField.setRelationType(StringUtils.camelize(s.replace("-", "_")));
                modelFields.add(entityField);
            }
        }

        return modelFields;
    }

    @Nullable
    private static PhpClass getEntityRepositoryClass(Project project, SymfonyBundle symfonyBundle, String classFqnName) {

        // some default bundle search path
        // Bundle/Resources/config/doctrine/Product.orm.yml
        // Bundle/Resources/config/doctrine/Product.mongodb.yml
        List<String[]> managerConfigs = new ArrayList<>();
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
    public static PhpClass resolveShortcutName(@NotNull Project project, @NotNull String shortcutName) {
        return resolveShortcutName(project, shortcutName, DoctrineTypes.Manager.ORM, DoctrineTypes.Manager.MONGO_DB, DoctrineTypes.Manager.COUCH_DB);
    }

    /**
     *
     * @param project PHPStorm projects
     * @param shortcutName name as MyBundle\Entity\Model or MyBundle:Model
     * @return null|PhpClass
     */
    @Nullable
    public static PhpClass resolveShortcutName(@NotNull Project project, @Nullable String shortcutName, DoctrineTypes.Manager... managers) {
        if(shortcutName == null) {
            return null;
        }

        // we dont need to resolve bundle name, use class name
        if (!shortcutName.contains(":")) {
            return PhpElementsUtil.getClassInterface(project, shortcutName);
        }

        // resolve:
        // MyBundle:Model -> MyBundle\Entity\Model
        // MyBundle:Folder\Model -> MyBundle\Entity\Folder\Model

        List<DoctrineTypes.Manager> managerList = Arrays.asList(managers);

        // collect entitymanager namespaces on bundle or container file
        Map<String, String> em = new HashMap<>();
        if(managerList.contains(DoctrineTypes.Manager.ORM)) {
            Map<String, String> entityNameMap = ServiceXmlParserFactory.getInstance(project, EntityNamesServiceParser.class).getEntityNameMap();
            em.putAll(entityNameMap);
            em.putAll(EntityHelper.getWeakBundleNamespaces(project, entityNameMap, "Entity"));
        }

        Map<String, String> odm = new HashMap<>();
        if(managerList.contains(DoctrineTypes.Manager.MONGO_DB) || managerList.contains(DoctrineTypes.Manager.COUCH_DB)) {
            Map<String, String> documentMap = ServiceXmlParserFactory.getInstance(project, DocumentNamespacesParser.class).getNamespaceMap();
            odm.putAll(documentMap);
            odm.putAll(EntityHelper.getWeakBundleNamespaces(project, documentMap, "Document"));
        }

        // split bundle and model name
        int firstDirectorySeparatorIndex = shortcutName.indexOf(":");
        String bundlename = shortcutName.substring(0, firstDirectorySeparatorIndex);
        String entityName = shortcutName.substring(firstDirectorySeparatorIndex + 1);

        // conditional find namespace on manager paths
        for(Map<String, String> map: Arrays.asList(em, odm)) {
            String namespace = map.get(bundlename);
            if(namespace == null) {
                continue;
            }

            PhpClass classInterface = PhpElementsUtil.getClassInterface(project, namespace + "\\" + entityName);
            if(classInterface != null) {
                return classInterface;
            }
        }

        return null;
    }

    @Nullable
    public static DoctrineTypes.Manager getManager(MethodReference methodReference) {

        PhpPsiElement phpTypedElement = methodReference.getFirstPsiChild();
        if(!(phpTypedElement instanceof PhpTypedElement)) {
            return null;
        }

        for(String typeString: PhpIndex.getInstance(methodReference.getProject()).completeType(methodReference.getProject(), ((PhpTypedElement) phpTypedElement).getType(), new HashSet<>()).getTypes()) {
            for(Map.Entry<DoctrineTypes.Manager, String> entry: DoctrineTypes.getManagerInstanceMap().entrySet()) {
                if(PhpElementsUtil.isInstanceOf(methodReference.getProject(), typeString, entry.getValue())) {
                    return entry.getKey();
                }
            }
        }

        return null;
    }

    public static PsiElement[] getModelPsiTargets(Project project, @NotNull String entityName) {
        List<PsiElement> results = new ArrayList<>();

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

        // relation type
        matcher = Pattern.compile("((Many|One)To(Many|One))\\(").matcher(text);
        if (matcher.find()) {
            doctrineModelField.setRelationType(matcher.group(1));

            // targetEntity name
            matcher = Pattern.compile("targetEntity[\\s]*=[\\s]*[\"|']([\\w_\\\\]+)[\"|']").matcher(text);
            if (matcher.find()) {
                doctrineModelField.setRelation(matcher.group(1));
            } else {
                // @TODO: external split
                // FLOW shortcut:
                // @var "\DateTime" is targetEntity
                PhpDocParamTag varTag = docBlock.getVarTag();
                if(varTag != null) {
                    String type = varTag.getType().toString();
                    if(org.apache.commons.lang.StringUtils.isNotBlank(type)) {
                        doctrineModelField.setRelation(type);
                    }
                }
            }
        }

        matcher = Pattern.compile("Column\\(").matcher(text);
        if (matcher.find()) {
            matcher = Pattern.compile("name\\s*=\\s*\"(\\w+)\"").matcher(text);
            if(matcher.find()) {
                doctrineModelField.setColumn(matcher.group(1));
            }

        }

    }

    /**
     * One PhpClass can have multiple targets and names @TODO: refactor
     */
    public static Collection<DoctrineModel> getModelClasses(final Project project) {

        HashMap<String, String> shortcutNames = new HashMap<String, String>() {{
            putAll(ServiceXmlParserFactory.getInstance(project, EntityNamesServiceParser.class).getEntityNameMap());
            putAll(ServiceXmlParserFactory.getInstance(project, DocumentNamespacesParser.class).getNamespaceMap());
        }};

        for (SymfonyBundle symfonyBundle : new SymfonyBundleUtil(project).getBundles()) {
            for(String s : new String[] {"Entity", "Document", "CouchDocument"}) {
                String namespace = symfonyBundle.getNamespaceName() + s;
                if(symfonyBundle.getRelative(s) != null || PhpIndex.getInstance(project).getNamespacesByName(namespace).size() > 0) {
                    shortcutNames.put(symfonyBundle.getName(), namespace);
                }
            }
        }

        // class fqn fallback
        Collection<DoctrineModel> doctrineModels = getModelClasses(project, shortcutNames);
        for (PhpClass phpClass : DoctrineMetadataUtil.getModels(project)) {
            if(containsDoctrineModelClass(doctrineModels, phpClass)) {
                continue;
            }

            doctrineModels.add(new DoctrineModel(phpClass));
        }

        DoctrineModelProviderParameter containerLoaderExtensionParameter = new DoctrineModelProviderParameter(project, new ArrayList<>());
        for(DoctrineModelProvider provider : EntityHelper.MODEL_POINT_NAME.getExtensions()) {
            for(DoctrineModelProviderParameter.DoctrineModel doctrineModel: provider.collectModels(containerLoaderExtensionParameter)) {
                doctrineModels.add(new DoctrineModel(doctrineModel.getPhpClass(), doctrineModel.getName()));
            }
        }

        return doctrineModels;
    }

    private static boolean containsDoctrineModelClass(@NotNull Collection<DoctrineModel> models, @NotNull PhpClass phpClass) {
        for (DoctrineModel doctrineModel : models) {
            if(PhpElementsUtil.isEqualClassName(doctrineModel.getPhpClass(), phpClass)) {
                return true;
            }
        }

        return false;
    }

    public static Collection<DoctrineModel> getModelClasses(Project project, Map<String, String> shortcutNames) {

        PhpClass repositoryInterface = PhpElementsUtil.getInterface(PhpIndex.getInstance(project), DoctrineTypes.REPOSITORY_INTERFACE);

        Collection<DoctrineModel> models = new ArrayList<>();
        for (Map.Entry<String, String> entry : shortcutNames.entrySet()) {
            for(PhpClass phpClass: PhpIndexUtil.getPhpClassInsideNamespace(project, entry.getValue())) {
                if(repositoryInterface != null && !isEntity(phpClass, repositoryInterface)) {
                    continue;
                }

                models.add(new DoctrineModel(phpClass, entry.getKey(), entry.getValue()));
            }
        }

        return models;
    }

    public static boolean isEntity(PhpClass entityClass, PhpClass repositoryClass) {

        if(entityClass.isAbstract() || entityClass.isInterface()) {
            return false;
        }

        return !PhpElementsUtil.isInstanceOf(entityClass, repositoryClass);
    }

    public static Map<String, String> getWeakBundleNamespaces(Project project, Map<String, String> entityNameMap, String subFolder) {

        Map<String, String> missingMap = new HashMap<>();

        Collection<SymfonyBundle> symfonyBundles = new SymfonyBundleUtil(project).getBundles();
        for(SymfonyBundle symfonyBundle: symfonyBundles) {
            if(symfonyBundle.isTestBundle()) {
                continue;
            }

            // namespace already known
            String bundleName = symfonyBundle.getName();
            if(entityNameMap.containsKey(bundleName)) {
               continue;
            }

            // find namepsace on file or class index
            String namespace = symfonyBundle.getNamespaceName() + subFolder;
            if(symfonyBundle.getRelative(subFolder) != null || PhpIndex.getInstance(project).getNamespacesByName(namespace).size() > 0) {
                missingMap.put(bundleName, namespace);
            }
        }

        return missingMap;
    }

}
