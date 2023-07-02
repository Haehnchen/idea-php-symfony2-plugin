package fr.adrienbrault.idea.symfony2plugin.twig.variable.collector;

import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Provide "symfony ux" component variables support
 *
 * @link https://symfony.com/bundles/ux-twig-component/current/index.html#exposeintemplate-attribute
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class UxComponentVariableCollector implements TwigFileVariableCollector {
    public void collectPsiVariables(@NotNull TwigFileVariableCollectorParameter parameter, @NotNull Map<String, PsiVariable> variables) {
        PsiFile psiFile = parameter.getElement().getContainingFile();
        if (!(psiFile instanceof TwigFile)) {
            return;
        }

        for (PhpClass phpClass : UxUtil.getComponentClassesForTemplateFile(parameter.getProject(), psiFile)) {
            UxUtil.visitComponentVariables(phpClass, pair -> variables.put(pair.getFirst(), new PsiVariable(pair.getSecond().getType().getTypes(), pair.getSecond())));
        }
    }
}
