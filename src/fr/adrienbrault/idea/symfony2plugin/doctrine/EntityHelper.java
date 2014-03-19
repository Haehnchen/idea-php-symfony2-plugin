package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.PhpNamedElementImpl;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.DocumentNamespacesParser;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.EntityNamesServiceParser;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineTypes;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlKeyFinder;
import fr.adrienbrault.idea.symfony2plugin.util.StringUtils;
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
            Matcher matcher = Pattern.compile("repositoryClass=[\"|'](.*)[\"|']").matcher(docAnnotationText);
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

    private static Set<String> getModelFieldsSet(YAMLKeyValue yamlKeyValue) {
        Set<String> fields = new HashSet<String>();

        for(YAMLKeyValue targetYamlKeyValue: getYamlModelFieldKeyValues(yamlKeyValue)) {
            Set<String> fieldSet = YamlHelper.getKeySet(targetYamlKeyValue);
            if(fieldSet != null) {
                fields.addAll(fieldSet);
            }
        }

        return fields;
    }

    public static Collection<YAMLKeyValue> getYamlModelFieldKeyValues(YAMLKeyValue yamlKeyValue) {
        Collection<YAMLKeyValue> keyValueCollection = new ArrayList<YAMLKeyValue>();

        for(String fieldMaps: new String[] { "fields", "manyToOne", "oneToOne", "manyToMany", "oneToMany"}) {
            YAMLKeyValue targetYamlKeyValue = YamlHelper.getYamlKeyValue(yamlKeyValue, fieldMaps);
            keyValueCollection.add(targetYamlKeyValue);
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
                    for(YAMLKeyValue yamlKeyValue: EntityHelper.getYamlModelFieldKeyValues((YAMLKeyValue) arrayKeyValue)) {
                        YAMLKeyValue target = YamlKeyFinder.findKey(yamlKeyValue, fieldName);
                        if(target != null) {
                            psiElements.add(target);
                        }
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
        for(SymfonyBundle symfonyBundle: new SymfonyBundleUtil(phpClass.getProject()).getBundles()) {
            for(String modelShortcut: new String[] {"orm", "mongodb"}) {
                String entityFile = "Resources/config/doctrine/" + phpClass.getName() + String.format(".%s.yml", modelShortcut);
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

    public static Set<String> getModelFields(PhpClass phpClass) {

        Set<String> modelFields = new HashSet<String>();

        PsiFile psiFile = getModelConfigFile(phpClass);

        if(psiFile instanceof YAMLFile) {
            PsiElement yamlDocument = psiFile.getFirstChild();
            if(yamlDocument instanceof YAMLDocument) {
                PsiElement arrayKeyValue = yamlDocument.getFirstChild();
                if(arrayKeyValue instanceof YAMLKeyValue) {
                    String className = YamlHelper.getYamlKeyName(((YAMLKeyValue) arrayKeyValue));
                    if(PhpElementsUtil.isEqualClassName(phpClass, className)) {
                        Set<String> fields = getModelFieldsSet((YAMLKeyValue) arrayKeyValue);
                        if(fields != null) {
                            modelFields.addAll(fields);
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

}
