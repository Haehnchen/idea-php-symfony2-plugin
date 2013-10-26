package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        List<ResolveResult> results = new ArrayList<ResolveResult>();

        for (PsiElement psiElement : RouteHelper.getMethods(getElement().getProject(), this.routeName)) {

            if(psiElement instanceof Method) {
                for(Parameter parameter: ((Method) psiElement).getParameters()) {
                    if(parameter.getName().equals(parameterName)) {
                        results.add(new PsiElementResolveResult(parameter));
                    }
                }
            }

            //results.add(new PsiElementResolveResult(psiElement));
        }

        return results.toArray(new ResolveResult[results.size()]);
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        List<LookupElement> lookupElements = new ArrayList<LookupElement>();

        Route route = RouteHelper.getRoute(getElement().getProject(), routeName);
        if(route == null) {
            return lookupElements.toArray();
        }

        for(String values: route.getVariables()) {
            lookupElements.add(LookupElementBuilder.create(values).withIcon(Symfony2Icons.ROUTE));
        }

        return lookupElements.toArray();
    }
}
