package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver;

import com.google.common.collect.Iterables;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class FormVarsResolver implements TwigTypeResolver {

    public void resolve(Collection<TwigTypeContainer> targets, Collection<TwigTypeContainer> previousElement, String typeName, Collection<List<TwigTypeContainer>> previousElements, @Nullable List<PsiVariable> psiVariables) {

        if(!"vars".equals(typeName) || previousElements.size() == 0) {
            return;
        }

        List<TwigTypeContainer> lastTwigTypeContainer = Iterables.getLast(previousElements);
        Symfony2InterfacesUtil symfony2InterfacesUtil = new Symfony2InterfacesUtil();

        for(TwigTypeContainer twigTypeContainer: lastTwigTypeContainer) {
            if(twigTypeContainer.getPhpNamedElement() instanceof PhpClass) {
                if(symfony2InterfacesUtil.isInstanceOf((PhpClass) twigTypeContainer.getPhpNamedElement(), "\\Symfony\\Component\\Form\\FormView")) {
                    targets.add(new TwigTypeContainer("action"));
                }
            }

        }

    }
}
