package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyCommand;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyCommandUtil {

    @NotNull
    public static Collection<SymfonyCommand> getCommands(@NotNull Project project) {

        Collection<SymfonyCommand> symfonyCommands = new ArrayList<>();

        for (PhpClass phpClass : PhpIndex.getInstance(project).getAllSubclasses("\\Symfony\\Component\\Console\\Command\\Command")) {

            if(PhpElementsUtil.isTestClass(phpClass)) {
                continue;
            }

            Method method = phpClass.findOwnMethodByName("configure");
            if(method == null) {
                continue;
            }

            PsiElement[] psiElements = PsiTreeUtil.collectElements(method, psiElement ->
                psiElement instanceof MethodReference && "setName".equals(((MethodReference) psiElement).getName())
            );

            for (PsiElement psiElement : psiElements) {

                if(!(psiElement instanceof MethodReference)) {
                    continue;
                }

                PsiElement psiMethodParameter = PsiElementUtils.getMethodParameterPsiElementAt((MethodReference) psiElement, 0);
                if(psiMethodParameter == null) {
                    continue;
                }

                String stringValue = PhpElementsUtil.getStringValue(psiMethodParameter);
                if(stringValue == null) {
                    continue;
                }

                symfonyCommands.add(new SymfonyCommand(stringValue, psiElement));
            }

        }

        return symfonyCommands;
    }

}
