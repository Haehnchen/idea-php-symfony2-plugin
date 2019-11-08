package fr.adrienbrault.idea.symfonyplugin.dic;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfonyplugin.util.completion.TagNameCompletionProvider;
import fr.adrienbrault.idea.symfonyplugin.util.dict.ServiceUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TagReference extends PsiPolyVariantReferenceBase<PsiElement> {

    private String tagName;

    public TagReference(PsiElement psiElement, String tagName) {
        super(psiElement);
        this.tagName = tagName;
    }

    public TagReference(@NotNull StringLiteralExpression element) {
        super(element);
        tagName = element.getContents();
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        return PsiElementResolveResult.createResults(ServiceUtil.getTaggedClassesWithCompiled(getElement().getProject(), this.tagName));
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return TagNameCompletionProvider.getTagLookupElements(getElement().getProject()).toArray();
    }

}
