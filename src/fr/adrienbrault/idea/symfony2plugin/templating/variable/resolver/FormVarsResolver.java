package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class FormVarsResolver implements TwigTypeResolver {

    public void resolve(Collection<TwigTypeContainer> targets, Collection<TwigTypeContainer> previousElement, String typeName, Collection<List<TwigTypeContainer>> previousElements, @Nullable List<PsiVariable> psiVariables) {

        if(!"vars".equals(typeName) || previousElements.size() == 0) {
            return;
        }

        List<TwigTypeContainer> lastTwigTypeContainer = null;
        for (Iterator collectionItr = previousElements.iterator(); collectionItr.hasNext(); ) {
            lastTwigTypeContainer = (List<TwigTypeContainer>) collectionItr.next();
        }

        Symfony2InterfacesUtil symfony2InterfacesUtil = new Symfony2InterfacesUtil();

        for(TwigTypeContainer twigTypeContainer: lastTwigTypeContainer) {
            if(twigTypeContainer.getPhpNamedElement() instanceof PhpClass) {
                if(symfony2InterfacesUtil.isInstanceOf((PhpClass) twigTypeContainer.getPhpNamedElement(), "\\Symfony\\Component\\Form\\FormView")) {
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
