package fr.adrienbrault.idea.symfony2plugin.asset;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetDirectoryReader;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetFile;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.ArrayList;
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
