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
        return isTag(psiElement, "custom_extends") ? getTemplateNames(psiElement, "custom_extends") : Collections.emptyList();
    }

    @Override
    public Collection<String> getIncludeTemplate(@NotNull PsiElement psiElement) {
        return isTag(psiElement, "custom_include") ? getTemplateNames(psiElement, "custom_include") : Collections.emptyList();
    }

    @Override
    public boolean isExtendsTemplate(@NotNull PsiElement psiElement) {
        return isTag(psiElement, "custom_extends");
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

    @NotNull
    @Override
    public Collection<String> getImportTemplate(@NotNull PsiElement psiElement) {
        return isTag(psiElement, "custom_import") ? getTemplateNames(psiElement, "custom_import") : Collections.emptyList();
    }

    @Override
    public boolean isImportTemplate(@NotNull PsiElement psiElement) {
        return isTag(psiElement, "custom_import");
    }

    @NotNull
    @Override
    public Collection<String> getFromTemplate(@NotNull PsiElement psiElement) {
        return isTag(psiElement, "custom_from") ? getTemplateNames(psiElement, "custom_from") : Collections.emptyList();
    }

    @Override
    public boolean isFromTemplate(@NotNull PsiElement psiElement) {
        return isTag(psiElement, "custom_from");
    }

    @NotNull
    @Override
    public Collection<String> getSourceTemplate(@NotNull PsiElement psiElement) {
        return isTag(psiElement, "custom_source") ? getTemplateNames(psiElement, "custom_source") : Collections.emptyList();
    }

    @Override
    public boolean isSourceTemplate(@NotNull PsiElement psiElement) {
        return isTag(psiElement, "custom_source");
    }

    @Override
    public boolean isTemplateFileReference(@NotNull PsiElement psiElement) {
        return isTag(psiElement, "custom_template")
            || isExtendsTemplate(psiElement)
            || isIncludeTemplate(psiElement)
            || isEmbedTemplate(psiElement)
            || isImportTemplate(psiElement)
            || isFromTemplate(psiElement)
            || isSourceTemplate(psiElement);
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
