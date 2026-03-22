package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.util.completion.TagNameCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TagReference extends PsiPolyVariantReferenceBase<PsiElement> {

    private final String tagName;

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
        if (DumbService.isDumb(getElement().getProject())) {
            return ResolveResult.EMPTY_ARRAY;
        }
        return PsiElementResolveResult.createResults(ServiceUtil.getTaggedClasses(getElement().getProject(), this.tagName));
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        if (DumbService.isDumb(getElement().getProject())) {
            return new Object[0];
        }
        return TagNameCompletionProvider.getTagLookupElements(getElement().getProject()).toArray();
    }

}
