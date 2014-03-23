package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.YamlRoutesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.YamlTranslationStubIndex;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationReference extends PsiPolyVariantReferenceBase<PsiElement> {

    private String domainName = null;
    private StringLiteralExpression element;

    public TranslationReference(@NotNull StringLiteralExpression element, String domain) {
        super(element);
        this.domainName = domain;
        this.element = element;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        List<ResolveResult> results = new ArrayList<ResolveResult>();
        PsiElement[] psiElements = TranslationUtil.getTranslationPsiElements(this.element.getProject(), this.element.getContents(), this.domainName);

        for (PsiElement psiElement : psiElements) {
            results.add(new PsiElementResolveResult(psiElement));
        }

        return results.toArray(new ResolveResult[results.size()]);
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return TranslationUtil.getTranslationLookupElementsOnDomain(getElement().getProject(), domainName).toArray();
    }

}
