package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PhpElementsUtil {
    static public List<ResolveResult> getClassInterfaceResolveResult(Project project, String FQNClassName) {

        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<PhpClass> phpClasses = phpIndex.getClassesByFQN(FQNClassName);
        Collection<PhpClass> phpInterfaces = phpIndex.getInterfacesByFQN(FQNClassName);

        List<ResolveResult> results = new ArrayList<ResolveResult>();
        for (PhpClass phpClass : phpClasses) {
            results.add(new PsiElementResolveResult(phpClass));
        }
        for (PhpClass phpInterface : phpInterfaces) {
            results.add(new PsiElementResolveResult(phpInterface));
        }

        return results;
    }
}
