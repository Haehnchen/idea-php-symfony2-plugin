package fr.adrienbrault.idea.symfonyplugin.config.component;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfonyplugin.dic.ContainerParameter;
import fr.adrienbrault.idea.symfonyplugin.stubs.ContainerCollectionResolver;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ParameterReference  extends PsiPolyVariantReferenceBase<PsiElement> {

    private String parameterName;

    public ParameterReference(@NotNull StringLiteralExpression element) {
        super(element);
        parameterName = element.getContents();
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        return new ResolveResult[0];
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        List<LookupElement> results = new ArrayList<>();

        for(Map.Entry<String, ContainerParameter> entry: ContainerCollectionResolver.getParameters(getElement().getProject()).entrySet()) {
            results.add(new ParameterLookupElement(entry.getValue()));
        }

        return results.toArray();
    }
}
