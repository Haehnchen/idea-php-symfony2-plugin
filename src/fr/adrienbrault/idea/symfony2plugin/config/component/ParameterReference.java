package fr.adrienbrault.idea.symfony2plugin.config.component;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.*;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceStringLookupElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ParameterReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {

    private String parameterName;

    public ParameterReference(@NotNull PsiElement element, String ParameterName) {
        super(element);
        parameterName = ParameterName;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {

        Symfony2ProjectComponent symfony2ProjectComponent = getElement().getProject().getComponent(Symfony2ProjectComponent.class);
        if (null == symfony2ProjectComponent) {
            return new ResolveResult[]{};
        }

        String parameterName = symfony2ProjectComponent.getConfigParameter().get(this.parameterName);

        if (null == parameterName) {
            return new ResolveResult[]{};
        }

        PhpIndex phpIndex = PhpIndex.getInstance(getElement().getProject());
        Collection<PhpClass> phpClasses = phpIndex.getClassesByFQN(parameterName);
        Collection<PhpClass> phpInterfaces = phpIndex.getInterfacesByFQN(parameterName);

        List<ResolveResult> results = new ArrayList<ResolveResult>();
        for (PhpClass phpClass : phpClasses) {
            results.add(new PsiElementResolveResult(phpClass));
        }
        for (PhpClass phpInterface : phpInterfaces) {
            results.add(new PsiElementResolveResult(phpInterface));
        }

        // self add; so variable is not marked as invalid eg in xml
        if(results.size() == 0) {
            results.add(new PsiElementResolveResult(getElement()));
        }

        return results.toArray(new ResolveResult[results.size()]);
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        ResolveResult[] resolveResults = multiResolve(false);

        return resolveResults.length == 1 ? resolveResults[0].getElement() : null;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        Symfony2ProjectComponent symfony2ProjectComponent = getElement().getProject().getComponent(Symfony2ProjectComponent.class);

        List<LookupElement> results = new ArrayList<LookupElement>();
        Map<String, String> it = symfony2ProjectComponent.getConfigParameter();

        for(Map.Entry<String, String> Entry: it.entrySet()) {
            results.add(new ParameterLookupElement(Entry.getKey(), Entry.getValue()));
        }

        return results.toArray();
    }
}
