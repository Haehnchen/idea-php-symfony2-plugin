package fr.adrienbrault.idea.symfony2plugin.config.component;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ParameterReference  extends PsiPolyVariantReferenceBase<PsiElement> {
    public ParameterReference(@NotNull StringLiteralExpression element) {
        super(element);
    }

    @NotNull
    @Override
    public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
        return ResolveResult.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public Object @NotNull [] getVariants() {

        List<LookupElement> results = new ArrayList<>();

        for(Map.Entry<String, ContainerParameter> entry: ContainerCollectionResolver.getParameters(getElement().getProject()).entrySet()) {
            results.add(new ParameterLookupElement(entry.getValue()));
        }

        return results.toArray();
    }
}
