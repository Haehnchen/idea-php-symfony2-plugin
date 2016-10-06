package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.LayeredIcon;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.elements.TwigExtendsTag;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;

public class TwigFileIconProvider extends com.intellij.ide.IconProvider {
    @Nullable
    @Override
    public Icon getIcon(@NotNull PsiElement psiElement, @Iconable.IconFlags int i) {
        if (!(psiElement instanceof TwigFile)) {
            return null;
        }

        TwigExtendsTag childOfType = PsiTreeUtil.findChildOfType(psiElement, TwigExtendsTag.class);
        if(childOfType != null) {
            return wrapIcon(((TwigFile) psiElement), Symfony2Icons.TWIG_IMPLEMENTS_FILE);
        }

        Collection<PsiFile> twigChild = TwigUtil.getTemplateFileReferences((TwigFile) psiElement, TwigHelper.getTemplateMap(psiElement.getProject(), true, false));
        if(twigChild.size() > 0) {
            return wrapIcon((TwigFile) psiElement, Symfony2Icons.TWIG_EXTENDS_FILE);
        }

        return null;
    }

    @Nullable
    private Icon wrapIcon(@NotNull TwigFile psiElement, @NotNull Icon twigExtendsFile) {
        Icon icon = psiElement.getFileType().getIcon();
        if (icon == null) {
            return null;
        }

        LayeredIcon rowIcon = new LayeredIcon(2);

        rowIcon.setIcon(icon, 0);
        rowIcon.setIcon(twigExtendsFile, 1, SwingConstants.NORTH_EAST);

        return rowIcon;
    }
}
