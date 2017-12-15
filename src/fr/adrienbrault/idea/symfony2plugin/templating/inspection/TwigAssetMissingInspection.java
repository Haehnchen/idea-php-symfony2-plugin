package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetDirectoryReader;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetFile;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.jetbrains.annotations.NotNull;

/**
 * asset('<caret>')
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigAssetMissingInspection extends LocalInspectionTool {
    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if(TwigPattern.getAutocompletableAssetPattern().accepts(element) && TwigUtil.isValidStringWithoutInterpolatedOrConcat(element)) {
                    invoke(element, holder);
                }

                super.visitElement(element);
            }
        };
    }

    private void invoke(@NotNull PsiElement element, @NotNull ProblemsHolder holder) {
        for (final AssetFile assetFile : new AssetDirectoryReader().setProject(element.getProject()).getAssetFiles()) {
            if(assetFile.toString().equals(element.getText())) {
                return;
            }
        }

        holder.registerProblem(element, "Missing asset");
    }
}
