package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.php.lang.psi.elements.PhpTypedElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.DocumentNamespacesParser;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.EntityNamesServiceParser;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineTypes;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EntityHelper {

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
                return PhpElementsUtil.getClass(PhpIndex.getInstance(project), matcher.group(1));
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

}
