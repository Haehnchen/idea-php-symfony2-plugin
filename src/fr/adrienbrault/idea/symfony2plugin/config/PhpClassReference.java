package fr.adrienbrault.idea.symfony2plugin.config;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.*;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceLookupElement;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceMap;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceStringLookupElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpClassReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {

    private String serviceId;

    public PhpClassReference(@NotNull PsiElement element, String ServiceId) {
        super(element);
        serviceId = ServiceId;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {

        PhpIndex phpIndex = PhpIndex.getInstance(getElement().getProject());
        Collection<PhpClass> phpClasses = phpIndex.getClassesByFQN(serviceId);

        List<ResolveResult> results = new ArrayList<ResolveResult>();
        for (PhpClass phpClass : phpClasses) {
            results.add(new PsiElementResolveResult(phpClass));
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
        List<LookupElement> results = new ArrayList<LookupElement>();
        // fill this
        return results.toArray();
    }
}
