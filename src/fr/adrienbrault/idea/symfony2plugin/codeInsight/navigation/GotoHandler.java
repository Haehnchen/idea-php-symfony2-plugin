package fr.adrienbrault.idea.symfony2plugin.codeInsight.navigation;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProviderInterface;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;


public class GotoHandler implements GotoDeclarationHandler {


    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor) {

        if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return new PsiElement[0];
        }

        Collection<PsiElement> psiTargets = new ArrayList<PsiElement>();
        for(GotoCompletionContributor contributor: GotoCompletionUtil.getContributors(psiElement)) {
            GotoCompletionProviderInterface formReferenceCompletionContributor = contributor.getProvider(psiElement);
            if(formReferenceCompletionContributor != null) {
                psiTargets.addAll(formReferenceCompletionContributor.getPsiTargets(psiElement));
            }
        }

        return psiTargets.toArray(new PsiElement[psiTargets.size()]);
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }
}
