package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TemplateReference extends PsiPolyVariantReferenceBase<PsiElement> {

    private String templateName;

    public TemplateReference(@NotNull StringLiteralExpression element) {
        super(element);
        templateName = element.getContents();
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        List<LookupElement> results = new ArrayList<LookupElement>();

        Map<String, TwigFile> twigFilesByName = TwigHelper.getTwigFilesByName(getElement().getProject());
        for (Map.Entry<String, TwigFile> entry : twigFilesByName.entrySet()) {
            results.add(
                new TemplateLookupElement(entry.getKey(), entry.getValue())
            );
        }

        return results.toArray();
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {

        PsiElement[] psiElements = TwigHelper.getTemplatePsiElements(getElement().getProject(), templateName);
        List<ResolveResult> results = new ArrayList<ResolveResult>();

        for (PsiElement psiElement : psiElements) {
            results.add(new PsiElementResolveResult(psiElement));
        }

        return results.toArray(new ResolveResult[results.size()]);
    }
}
