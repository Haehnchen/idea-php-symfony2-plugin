package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder.FormViewDataHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormVarsResolver implements TwigTypeResolver {
    public void resolve(@NotNull Project project, Collection<TwigTypeContainer> targets, Collection<TwigTypeContainer> previousElement, String typeName, Collection<List<TwigTypeContainer>> previousElements, @Nullable Collection<PsiVariable> psiVariables) {
        if(!"vars".equals(typeName) || previousElements.isEmpty()) {
            return;
        }

        List<TwigTypeContainer> lastTwigTypeContainer = null;
        for (List<TwigTypeContainer> element : previousElements) {
            lastTwigTypeContainer = element;
        }

        for (TwigTypeContainer twigTypeContainer: lastTwigTypeContainer) {
            FormViewDataHolder formViewDataHolder = twigTypeContainer.getFormViewDataHolder();
            if (formViewDataHolder != null && !formViewDataHolder.formTypeFqns().isEmpty()) {
                attachVars(project, targets);
            }
        }
    }

    private void attachVars(Project project, Collection<TwigTypeContainer> targets) {
        for(String string: FormOptionsUtil.getFormViewVars(project, "form")) {
            targets.add(new TwigTypeContainer(string));
        }
    }
}
