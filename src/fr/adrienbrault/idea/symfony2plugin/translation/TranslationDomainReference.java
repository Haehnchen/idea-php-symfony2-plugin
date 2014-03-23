package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.psi.*;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationDomainReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {

    private String domainName = null;

    public TranslationDomainReference(@NotNull StringLiteralExpression element) {
        super(element);
        this.domainName = element.getContents();
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        return PsiElementResolveResult.createResults(TranslationUtil.getDomainPsiFiles(getElement().getProject(), this.domainName));
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        return null;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return TranslationUtil.getTranslationDomainLookupElements(getElement().getProject()).toArray();
    }

}
