package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateGotoCompletionRegistrar extends GotoCompletionProvider {
    public TemplateGotoCompletionRegistrar(PsiElement element) {
        super(element);
    }

    @NotNull
    @Override
    public Collection<LookupElement> getLookupElements() {
        return TwigHelper.getAllTemplateLookupElements(getElement().getProject());
    }

    @NotNull
    @Override
    public Collection<PsiElement> getPsiTargets(PsiElement element) {
        String templateName = GotoCompletionUtil.getTextValueForElement(element);
        if(templateName == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(
            TwigHelper.getTemplatePsiElements(getElement().getProject(), templateName)
        );
    }
}
