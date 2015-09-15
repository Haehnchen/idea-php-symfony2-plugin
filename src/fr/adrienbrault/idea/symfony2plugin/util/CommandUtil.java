package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class CommandUtil {

    public static Map<String, String> getCommandHelper(@NotNull Project project) {

        Map<String, String> map = new HashMap<String, String>();
        for(PhpClass phpClass: PhpIndex.getInstance(project).getAllSubclasses("\\Symfony\\Component\\Console\\Helper\\HelperInterface")) {
            String helperName = PhpElementsUtil.getMethodReturnAsString(phpClass, "getName");
            if(helperName != null) {
                String presentableFQN = phpClass.getPresentableFQN();
                if(presentableFQN != null) {
                    map.put(helperName, presentableFQN);
                }
            }
        }

        return map;
    }
}
