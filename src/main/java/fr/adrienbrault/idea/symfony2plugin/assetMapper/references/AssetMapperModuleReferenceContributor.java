package fr.adrienbrault.idea.symfony2plugin.assetMapper.references;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.javascript.frameworks.modules.JSResolvableModuleReferenceContributor;
import com.intellij.lang.javascript.psi.resolve.JSResolveResult;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.assetMapper.AssetMapperUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AssetMapperModuleReferenceContributor extends JSResolvableModuleReferenceContributor {
    @Override
    protected ResolveResult @NotNull [] resolveElement(@NotNull PsiElement psiElement, @NotNull String module) {
        if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return new ResolveResult[0];
        }

        Collection<VirtualFile> files = AssetMapperUtil.getModuleReferences(psiElement.getProject(), module);

        if (files.isEmpty()) {
            return new ResolveResult[0];
        }

        return JSResolveResult.toResolveResults(PsiElementUtils.convertVirtualFilesToPsiFiles(psiElement.getProject(), files));
    }

    @Override
    public @NotNull Collection<LookupElement> getLookupElements(@NotNull String unquotedEscapedText, @NotNull PsiElement host) {
        if (!Symfony2ProjectComponent.isEnabled(host)) {
            return Collections.emptyList();
        }

        return AssetMapperUtil.getLookupElements(host.getProject());
    }

    @Override
    public int getDefaultWeight() {
        return 10;
    }
}
