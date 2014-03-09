package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapClassReference extends PsiPolyVariantReferenceBase<PsiElement> {

    private Map<String, String> variants;

    public MapClassReference(@NotNull StringLiteralExpression element, Map<String, String> variants) {
        super(element);
        this.variants = variants;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {

        List<ResolveResult> resolveResults = new ArrayList<ResolveResult>();

        for (Map.Entry<String, String> entry: this.variants.entrySet()) {
            PhpClass phpClass = PhpElementsUtil.getClassInterface(getElement().getProject(), entry.getValue());
            if(phpClass != null) {
                resolveResults.add(new PsiElementResolveResult(phpClass));
            }
        }


        return resolveResults.toArray(new ResolveResult[resolveResults.size()]);
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        List<LookupElement> lookupElements = new ArrayList<LookupElement>();
        for (Map.Entry<String, String> entry: this.variants.entrySet()) {
            PhpClass phpClass = PhpElementsUtil.getClassInterface(getElement().getProject(), entry.getValue());
            if(phpClass != null) {
                lookupElements.add(LookupElementBuilder.create(entry.getKey()).withIcon(PhpIcons.CLASS).withTypeText(phpClass.getPresentableFQN()));
            }
        }

        return lookupElements.toArray();

    }
}
