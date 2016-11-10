package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteGotoCompletionProvider extends GotoCompletionProvider {
    public RouteGotoCompletionProvider(PsiElement element) {
        super(element);
    }

    @NotNull
    @Override
    public Collection<LookupElement> getLookupElements() {
        return RouteHelper.getRoutesLookupElements(getElement().getProject());
    }

    @NotNull
    @Override
    public Collection<PsiElement> getPsiTargets(PsiElement element) {
        String value = GotoCompletionUtil.getTextValueForElement(element);
        if(value == null) {
            return Collections.emptyList();
        }

        return RouteHelper.getRouteDefinitionTargets(getElement().getProject(), value);
    }
}
