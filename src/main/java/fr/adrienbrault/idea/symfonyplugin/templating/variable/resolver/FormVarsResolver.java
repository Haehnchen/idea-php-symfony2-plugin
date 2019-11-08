package fr.adrienbrault.idea.symfonyplugin.templating.variable.resolver;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfonyplugin.form.util.FormOptionsUtil;
import fr.adrienbrault.idea.symfonyplugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfonyplugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfonyplugin.util.PhpElementsUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormVarsResolver implements TwigTypeResolver {
    public void resolve(Collection<TwigTypeContainer> targets, Collection<TwigTypeContainer> previousElement, String typeName, Collection<List<TwigTypeContainer>> previousElements, @Nullable Collection<PsiVariable> psiVariables) {
        if(!"vars".equals(typeName) || previousElements.size() == 0) {
            return;
        }

        List<TwigTypeContainer> lastTwigTypeContainer = null;
        for (Iterator collectionItr = previousElements.iterator(); collectionItr.hasNext(); ) {
            lastTwigTypeContainer = (List<TwigTypeContainer>) collectionItr.next();
        }

        for(TwigTypeContainer twigTypeContainer: lastTwigTypeContainer) {
            if(twigTypeContainer.getPhpNamedElement() instanceof PhpClass) {
                if(PhpElementsUtil.isInstanceOf((PhpClass) twigTypeContainer.getPhpNamedElement(), "\\Symfony\\Component\\Form\\FormView")) {
                    attachVars(twigTypeContainer.getPhpNamedElement().getProject(), targets);
                }
            }

        }

    }

    private void attachVars(Project project, Collection<TwigTypeContainer> targets) {
        for(String string: FormOptionsUtil.getFormViewVars(project, "form")) {
            targets.add(new TwigTypeContainer(string));
        }
    }
}
