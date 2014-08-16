package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
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
        return TwigHelper.getAllTemplateLookupElements(getElement().getProject()).toArray();
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
