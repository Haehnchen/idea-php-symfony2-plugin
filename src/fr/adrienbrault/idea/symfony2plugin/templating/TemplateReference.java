package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigHelper;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TemplateReference extends PsiPolyVariantReferenceBase<PsiElement> {
    @NotNull
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
        return PsiElementResolveResult
            .createResults(TwigHelper.getTemplatePsiElements(getElement().getProject(), templateName));
    }
}
