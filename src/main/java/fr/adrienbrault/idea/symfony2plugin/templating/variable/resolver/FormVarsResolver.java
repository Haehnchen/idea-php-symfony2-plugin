package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormVarsResolver implements TwigTypeResolver {
    public void resolve(Collection<TwigTypeContainer> targets, Collection<TwigTypeContainer> previousElement, String typeName, Collection<List<TwigTypeContainer>> previousElements, @Nullable Collection<PsiVariable> psiVariables) {
        if(!"vars".equals(typeName) || previousElements.isEmpty()) {
            return;
        }

        List<TwigTypeContainer> lastTwigTypeContainer = null;
        for (List<TwigTypeContainer> element : previousElements) {
            lastTwigTypeContainer = element;
        }

        for (TwigTypeContainer twigTypeContainer: lastTwigTypeContainer) {
            if (twigTypeContainer.getPhpNamedElement() instanceof PhpClass) {
                if (FormFieldResolver.isFormView((PhpClass) twigTypeContainer.getPhpNamedElement())) {
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
