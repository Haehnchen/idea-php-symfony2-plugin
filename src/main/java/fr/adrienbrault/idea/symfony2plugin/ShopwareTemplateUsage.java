package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigFileUsage;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ShopwareTemplateUsage implements TwigFileUsage {
    @Override
    public Collection<String> getExtendsTemplate(@NotNull PsiElement psiElement) {
        return getTemplateParameter(psiElement, "sw_extends");
    }

    @Override
    public Collection<String> getIncludeTemplate(@NotNull PsiElement psiElement) {
        return getTemplateParameter(psiElement, "sw_include");
    }

    @Override
    public boolean isExtendsTemplate(@NotNull PsiElement psiElement) {
        return isTag(psiElement, "sw_extends");
    }

    @Override
    public boolean isIncludeTemplate(@NotNull PsiElement psiElement) {
        return isTag(psiElement, "sw_include");
    }

    private boolean isTag(@NotNull PsiElement psiElement, @NotNull String tag) {
        if (psiElement.getNode().getElementType() == TwigElementTypes.TAG) {
            PsiElement tagElement = PsiElementUtils.getChildrenOfType(psiElement, PlatformPatterns.psiElement().withElementType(TwigTokenTypes.TAG_NAME));
            if (tagElement != null) {
                return tag.equals(tagElement.getText());
            }
        }

        return false;
    }

    @Nullable
    private Collection<String> getTemplateParameter(@NotNull PsiElement psiElement, @NotNull String tag) {
        if (psiElement.getNode().getElementType() == TwigElementTypes.TAG) {
            PsiElement swImportTag = PsiElementUtils.getChildrenOfType(psiElement, TwigPattern.getTagNameParameterPattern(TwigElementTypes.TAG, tag));
            if (swImportTag != null) {
                String templateName = swImportTag.getText();
                if (StringUtils.isNotBlank(templateName)) {
                    return Collections.singletonList(templateName);
                }
            }
        }

        return null;
    }
}
