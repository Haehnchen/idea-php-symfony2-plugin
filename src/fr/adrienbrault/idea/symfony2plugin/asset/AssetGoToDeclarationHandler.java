package fr.adrienbrault.idea.symfony2plugin.asset;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AssetGoToDeclarationHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor) {

        if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return null;
        }

        // remove this call, if withName Pattern is working
        if (!isValidTag(psiElement, "stylesheets", "javascripts")) {
            return null;
        }

        List<VirtualFile> virtualFiles = TwigHelper.resolveAssetsFiles(psiElement.getProject(), psiElement.getText(), null);

        List<PsiElement> psiElements = new ArrayList<PsiElement>();
        for (VirtualFile virtualFile : virtualFiles) {
            psiElements.add(PsiManager.getInstance(psiElement.getProject()).findFile(virtualFile));
        }

        return psiElements.toArray(new PsiElement[psiElements.size()]);
    }

    private boolean isValidTag(PsiElement psiElement, String... tags) {
        List<ElementPattern<PsiElement>> patterns = new ArrayList<ElementPattern<PsiElement>>();
        patterns.add(TwigHelper.getAutocompletableAssetPattern());

        for (String tag: tags) {
            patterns.add(TwigHelper.getAutocompletableAssetTag(tag));
        }

        return PlatformPatterns
            .or(patterns.toArray(new ElementPattern<?>[patterns.size()]))
            .accepts(psiElement)
        ;
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }
}
