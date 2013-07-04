package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.EntityNamesServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
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

            // search for @ORM\Entity(repositoryClass="Foo\Bar\RegisterRepository")
            String docAnnotationText = docAnnotation.getText();
            Matcher matcher = Pattern.compile("repositoryClass=[\"|'](.*)[\"|']").matcher(docAnnotationText);
            if (matcher.find()) {
                //System.out.println("Annotation: " + shortcutName);
                return PhpElementsUtil.getClass(PhpIndex.getInstance(project), matcher.group(1));
            }
        }

        SymfonyBundle symfonyBundle = new SymfonyBundleUtil(PhpIndex.getInstance(project)).getContainingBundle(phpClass);
        if(symfonyBundle != null) {
            String classFqnName = phpClass.getPresentableFQN();

            if(classFqnName != null) {
                String entityName = classFqnName.substring(symfonyBundle.getNamespaceName().length() - 1);
                if(entityName.startsWith("Entity\\")) {
                    entityName =  entityName.substring("Entity\\".length());
                }

                String entityFile = "Resources/config/doctrine/" + entityName + ".orm.yml";
                VirtualFile virtualFile = symfonyBundle.getRelative(entityFile);
                if(virtualFile != null) {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                    if(psiFile != null) {

                        // search for "repositoryClass: Foo\Bar\RegisterRepository" also provide quoted values
                        Matcher matcher = Pattern.compile("[\\s]*repositoryClass:[\\s]*[\"|']*(.*)[\"|']*").matcher(psiFile.getText());
                        if (matcher.find()) {
                            //System.out.println("Yml: " + shortcutName);
                            return PhpElementsUtil.getClass(PhpIndex.getInstance(project), matcher.group(1));
                        }

                        // we found entity config so no other check needed
                        return null;
                    }

                }
            }

        }

        // old __CLASS__ Repository type
        // @TODO remove this fallback when we implemented all cases
        return resolveShortcutName(project, shortcutName + "Repository");
    }

    /**
     *
     * @param project PHPStorm projects
     * @param shortcutName name as MyBundle\Entity\Model or MyBundle:Model
     * @return null|PhpClass
     */
    @Nullable
    public static PhpClass resolveShortcutName(Project project, String shortcutName) {

        if(shortcutName == null) {
            return null;
        }

        String entity_name = shortcutName;

        // resolve:
        // MyBundle:Model -> MyBundle\Entity\Model
        // MyBundle:Folder\Model -> MyBundle\Entity\Folder\Model
        if (shortcutName.contains(":")) {

            Map<String, String> em = ServiceXmlParserFactory.getInstance(project, EntityNamesServiceParser.class).getEntityNameMap();

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

}
