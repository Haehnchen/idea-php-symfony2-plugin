package fr.adrienbrault.idea.symfony2plugin.asset;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetDirectoryReader;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetFile;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Daniel Esoendiller <daniel@espendiller.net>
 */
public class AssetGoToDeclarationHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor) {

        // remove this call, if withName Pattern is working
        if (!isValidTag(psiElement, "stylesheets", "javascripts")) {
            return null;
        }

        List<AssetFile> assetFiles = new AssetDirectoryReader().setIncludeBundleDir(true).setProject(psiElement.getProject()).getAssetFiles();

        for(AssetFile file: assetFiles) {
            if(file.toString().equals(psiElement.getText())) {
                return new PsiElement[] { PsiManager.getInstance(psiElement.getProject()).findFile(file.getFile()) };
            }
        }

        return null;
    }

    private boolean isValidTag(PsiElement psiElement, String... tags) {
        for(String tag: tags) {
            if(TwigHelper.getAutocompletableAssetTag(tag).accepts(psiElement)) {
               return true;
            }
        }

        return false;
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }
}
