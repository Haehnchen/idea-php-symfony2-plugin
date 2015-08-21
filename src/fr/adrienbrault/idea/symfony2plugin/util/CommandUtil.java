package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpReturn;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CommandUtil {

    public static Map<String, String> getCommandHelper(Project project) {

        Map<String, String> map = new HashMap<String, String>();
        Collection<PhpClass> phpClasses = PhpIndex.getInstance(project).getAllSubclasses("\\Symfony\\Component\\Console\\Helper\\HelperInterface");
        for(PhpClass phpClass: phpClasses) {
            Method method = phpClass.findMethodByName("getName");
            for(PhpReturn phpReturn: PsiTreeUtil.findChildrenOfType(method, PhpReturn.class)) {
                String nameValue = PhpElementsUtil.getStringValue(phpReturn.getArgument());
                if(nameValue != null) {
                    String presentableFQN = phpClass.getPresentableFQN();
                    if(presentableFQN != null) {
                        map.put(nameValue, presentableFQN);
                    }
                }
            }
        }

        return map;
    }


}
