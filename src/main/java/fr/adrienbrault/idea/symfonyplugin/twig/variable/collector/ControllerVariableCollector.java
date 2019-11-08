package fr.adrienbrault.idea.symfonyplugin.twig.variable.collector;

import com.intellij.psi.PsiFile;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfonyplugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfonyplugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfonyplugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfonyplugin.templating.variable.dict.PsiVariable;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ControllerVariableCollector implements TwigFileVariableCollector {
    @Override
    public void collectPsiVariables(@NotNull TwigFileVariableCollectorParameter parameter, @NotNull Map<String, PsiVariable> variables) {
        PsiFile psiFile = parameter.getElement().getContainingFile();
        if(!(psiFile instanceof TwigFile)) {
            return;
        }

        variables.putAll(TwigUtil.collectControllerTemplateVariables((TwigFile) psiFile));
    }
}
