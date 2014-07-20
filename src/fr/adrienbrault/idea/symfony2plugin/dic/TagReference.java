package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.resolve.PhpResolveResult;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.completion.TagNameCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

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
        return PhpResolveResult.createResults(ServiceUtil.getTaggedClassesWithCompiled(getElement().getProject(), this.tagName));
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return TagNameCompletionProvider.getTagLookupElements(getElement().getProject()).toArray();
    }

}
