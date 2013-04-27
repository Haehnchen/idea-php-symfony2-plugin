package fr.adrienbrault.idea.symfony2plugin.config.component;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ParameterReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {

    private String parameterName;

    private boolean wrapPercent = false;

    public ParameterReference(@NotNull PsiElement element, String ParameterName) {
        super(element);
        parameterName = ParameterName;
    }

    public ParameterReference wrapVariantsWithPercent(boolean WrapPercent) {
        this.wrapPercent = WrapPercent;
        return this;
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

        List<ResolveResult> results = new ArrayList<ResolveResult>();
        results.addAll(PhpElementsUtil.getClassInterfaceResolveResult(getElement().getProject(), parameterName));

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
            String parameterKey = Entry.getKey();

            // wrap parameter for reuse this class in xml, php and yaml
            if(this.wrapPercent) {
                parameterKey = "%" + parameterKey + "%";
            }

            results.add(new ParameterLookupElement(parameterKey, Entry.getValue()));
        }

        return results.toArray();
    }
}
