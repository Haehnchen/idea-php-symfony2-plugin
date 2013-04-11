package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.twig.TwigFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TemplateReference extends PsiReferenceBase<PsiElement> implements PsiReference {

    private String templateName;

    public TemplateReference(@NotNull StringLiteralExpression element) {
        super(element);

        templateName = element.getText().substring(
            element.getValueRange().getStartOffset(),
            element.getValueRange().getEndOffset()
        ); // Remove quotes
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        Map<String, TwigFile> twigFilesByName = TemplateHelper.getTwigFilesByName(getElement().getProject());

        return twigFilesByName.get(templateName);
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        List<LookupElement> results = new ArrayList<LookupElement>();

        Map<String, TwigFile> twigFilesByName = TemplateHelper.getTwigFilesByName(getElement().getProject());
        for (Map.Entry<String, TwigFile> entry : twigFilesByName.entrySet()) {
            results.add(
                new TemplateLookupElement(entry.getKey(), entry.getValue())
            );
        }

        return results.toArray();
    }

}
