package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StringArrayListReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {

    private ArrayList<String> variants;

    public StringArrayListReference(@NotNull StringLiteralExpression element, ArrayList<String> variants) {
        super(element);
        this.variants = variants;
    }

    public StringArrayListReference(@NotNull StringLiteralExpression element, String[] variants) {
        super(element);
        this.variants = new ArrayList<String>(Arrays.asList(variants));
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        return new ResolveResult[0];
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
        for (String variant: this.variants) {
            lookupElements.add(LookupElementBuilder.create(variant));
        }

        return lookupElements.toArray();

    }
}
