package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ModelFieldReference extends PsiPolyVariantReferenceBase<PsiElement> {

    private Collection<PhpClass> phpClasses;
    private String fieldName;

    public ModelFieldReference(StringLiteralExpression psiElement, Collection<PhpClass> phpClasses) {
        super(psiElement);
        this.phpClasses = phpClasses;
        this.fieldName = psiElement.getContents();
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        List<ResolveResult> results = new ArrayList<ResolveResult>();

        for(PhpClass phpClass: phpClasses) {
            for(PsiElement psiElement: EntityHelper.getModelFieldTargets(phpClass, fieldName)) {
                results.add(new PsiElementResolveResult(psiElement));
            }
        }

        return results.toArray(new ResolveResult[results.size()]);
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        List<LookupElement> results = new ArrayList<LookupElement>();

        for(PhpClass phpClass: phpClasses) {
            for(String field: EntityHelper.getModelFields(phpClass)) {
                results.add(LookupElementBuilder.create(field));
            }
        }

        return results.toArray();
    }

}
