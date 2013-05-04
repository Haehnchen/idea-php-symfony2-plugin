package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PhpElementsUtil {
    static public List<ResolveResult> getClassInterfaceResolveResult(Project project, String FQNClassOrInterfaceName) {

        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<PhpClass> phpClasses = phpIndex.getClassesByFQN(FQNClassOrInterfaceName);
        Collection<PhpClass> phpInterfaces = phpIndex.getInterfacesByFQN(FQNClassOrInterfaceName);

        List<ResolveResult> results = new ArrayList<ResolveResult>();
        for (PhpClass phpClass : phpClasses) {
            results.add(new PsiElementResolveResult(phpClass));
        }
        for (PhpClass phpInterface : phpInterfaces) {
            results.add(new PsiElementResolveResult(phpInterface));
        }

        return results;
    }

    static public PsiElement[] getClassInterfacePsiElements(Project project, String FQNClassOrInterfaceName) {

        // convert ResolveResult to PsiElement
        List<PsiElement> results = new ArrayList<PsiElement>();
        for(ResolveResult result: getClassInterfaceResolveResult(project, FQNClassOrInterfaceName)) {
            results.add(result.getElement());
        }

        return results.toArray(new PsiElement[results.size()]);
    }


}
