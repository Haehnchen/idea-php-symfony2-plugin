package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EntityHelper {

    /**
     *
     * @param project PHPStorm projects
     * @param shortcutName name as MyBundle\Entity\Model or MyBundle:Model
     * @return null|PhpClass
     */
    public static PhpClass resolveShortcutName(Project project, String shortcutName) {

        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<PhpClass> phpClasses = phpIndex.getAllSubclasses("\\Symfony\\Component\\HttpKernel\\Bundle\\Bundle");

        Map<String, String> bundlesDirectories = new HashMap<String, String>();
        for (PhpClass phpClass : phpClasses) {
            bundlesDirectories.put(phpClass.getName(), phpClass.getNamespaceName());
        }

        String entity_name = null;

        // resolve:
        // MyBundle:Model -> MyBundle\Entity\Model
        // MyBundle:Folder\Model -> MyBundle\Entity\Folder\Model
        if (shortcutName.contains(":")) {

            int firstDirectorySeparatorIndex = shortcutName.indexOf(":");

            String bundlename = shortcutName.substring(0, firstDirectorySeparatorIndex);
            String entityName = shortcutName.substring(firstDirectorySeparatorIndex + 1);

            String namespace = bundlesDirectories.get(bundlename);

            if(namespace == null) {
                return null;
            }

            entity_name = namespace + "Entity\\" + entityName;

        }  else {
            entity_name = shortcutName;
        }

        // dont we have any unique class getting method here?
        Collection<PhpClass> entity_classes = phpIndex.getClassesByFQN(entity_name);
        if(!entity_classes.isEmpty()){
            return entity_classes.iterator().next();
        }

        return null;
    }

}
