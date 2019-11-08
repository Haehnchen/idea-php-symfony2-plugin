package fr.adrienbrault.idea.symfonyplugin.routing;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteParameterReference extends PsiPolyVariantReferenceBase<PsiElement> {

    private String routeName;
    private String parameterName;

    public RouteParameterReference(@NotNull StringLiteralExpression element, String routeName) {
        super(element);
        this.routeName = routeName;
        this.parameterName = element.getContents();
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {

        List<ResolveResult> results = new ArrayList<>();

        for (PsiElement psiParameter : RouteHelper.getRouteParameterPsiElements(getElement().getProject(), this.routeName, parameterName)) {
            results.add(new PsiElementResolveResult(psiParameter));
        }

        return results.toArray(new ResolveResult[results.size()]);
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return RouteHelper.getRouteParameterLookupElements(getElement().getProject(), routeName);
    }
}
