package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class RouteReference extends PsiPolyVariantReferenceBase<PsiElement> {

    private final String routeName;

    public RouteReference(@NotNull StringLiteralExpression element) {
        super(element);
        this.routeName = element.getContents();
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        return PsiElementResolveResult.createResults(RouteHelper.getRouteDefinitionTargets(getElement().getProject(), this.routeName));
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return RouteHelper.getRoutesLookupElements(getElement().getProject()).toArray();
    }

}
