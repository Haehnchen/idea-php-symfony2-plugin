package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.*;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationDomainReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {

    private String domainName = null;
    private StringLiteralExpression element;

    public TranslationDomainReference(@NotNull StringLiteralExpression element) {
        super(element);
        this.domainName = element.getContents();
        this.element = element;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        List<ResolveResult> results = new ArrayList<ResolveResult>();
        PsiElement[] psiElements = TranslationUtil.getDomainFilePsiElements(this.element.getProject(), domainName);

        for (PsiElement psiElement : psiElements) {
            results.add(new PsiElementResolveResult(psiElement));
        }

        return results.toArray(new ResolveResult[results.size()]);
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        return null;
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        List<LookupElement> lookupElements = new ArrayList<LookupElement>();

        TranslationStringMap map = TranslationIndex.getInstance(getElement().getProject()).getTranslationMap();
        for(String domainKey : map.getDomainList()) {
            lookupElements.add(new TranslatorLookupElement(domainKey, domainKey));
        }

        return lookupElements.toArray();
    }

}
