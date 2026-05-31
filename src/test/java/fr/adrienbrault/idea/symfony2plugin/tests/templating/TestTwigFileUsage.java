package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigFileUsage;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public class TestTwigFileUsage implements TwigFileUsage {
    @Override
    public Collection<String> getExtendsTemplate(@NotNull PsiElement psiElement) {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getIncludeTemplate(@NotNull PsiElement psiElement) {
        return isTag(psiElement, "custom_include") ? getTemplateNames(psiElement, "custom_include") : Collections.emptyList();
    }

    @Override
    public boolean isExtendsTemplate(@NotNull PsiElement psiElement) {
        return false;
    }

    @Override
    public boolean isIncludeTemplate(@NotNull PsiElement psiElement) {
        return isTag(psiElement, "custom_include");
    }

    @NotNull
    @Override
    public Collection<String> getEmbedTemplate(@NotNull PsiElement psiElement) {
        return isTag(psiElement, "custom_embed") ? getTemplateNames(psiElement, "custom_embed") : Collections.emptyList();
    }

    @Override
    public boolean isEmbedTemplate(@NotNull PsiElement psiElement) {
        return isTag(psiElement, "custom_embed");
    }

    @Override
    public boolean isTemplateFileReference(@NotNull PsiElement psiElement) {
        return isTag(psiElement, "custom_template") || isIncludeTemplate(psiElement) || isEmbedTemplate(psiElement);
    }

    private static Collection<String> getTemplateNames(@NotNull PsiElement psiElement, @NotNull String tagName) {
        PsiElement templateName = PsiElementUtils.getChildrenOfType(psiElement, PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
            .afterLeafSkipping(
                TwigPattern.STRING_WRAP_PATTERN,
                PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText(tagName)
            )
        );

        return templateName == null ? Collections.emptyList() : Collections.singleton(templateName.getText());
    }

    private static boolean isTag(@NotNull PsiElement psiElement, @NotNull String name) {
        if (psiElement.getNode().getElementType() == TwigElementTypes.TAG) {
            PsiElement tagElement = PsiElementUtils.getChildrenOfType(psiElement, PlatformPatterns.psiElement().withElementType(TwigTokenTypes.TAG_NAME));
            if (tagElement != null) {
                return name.equals(tagElement.getText());
            }
        }

        return false;
    }
}
