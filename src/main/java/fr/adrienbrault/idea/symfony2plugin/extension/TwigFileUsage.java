package fr.adrienbrault.idea.symfony2plugin.extension;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * Template usage provider for custom Twig constructs.
 *
 * <p>Callers pass candidate Twig PSI elements to this interface. For tag-based template usages
 * such as custom include or extends tags, callers must pass the full Twig tag composite element
 * ({@code com.jetbrains.twig.elements.TwigElementTypes.TAG}), not the string literal leaf
 * ({@code com.jetbrains.twig.TwigTokenTypes.STRING_TEXT}).</p>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface TwigFileUsage {
    /**
     * Returns template names from an extends-like usage.
     */
    Collection<String> getExtendsTemplate(@NotNull PsiElement psiElement);

    /**
     * Returns template names from an include-like usage.
     */
    Collection<String> getIncludeTemplate(@NotNull PsiElement psiElement);

    /**
     * Checks whether the given element is an extends-like template usage.
     */
    boolean isExtendsTemplate(@NotNull PsiElement psiElement);

    /**
     * Checks whether the given element is an include-like template usage.
     */
    boolean isIncludeTemplate(@NotNull PsiElement psiElement);

    /**
     * Returns template names from an embed-like usage.
     */
    @NotNull
    default Collection<String> getEmbedTemplate(@NotNull PsiElement psiElement) {
        return Collections.emptyList();
    }

    /**
     * Checks whether the given element is an embed-like template usage.
     */
    default boolean isEmbedTemplate(@NotNull PsiElement psiElement) {
        return false;
    }

    /**
     * Returns template names from a use-like usage.
     */
    @NotNull
    default Collection<String> getUseTemplate(@NotNull PsiElement psiElement) {
        return Collections.emptyList();
    }

    /**
     * Checks whether the given element is a use-like template usage.
     */
    default boolean isUseTemplate(@NotNull PsiElement psiElement) {
        return false;
    }

    /**
     * Returns template names from an import-like usage.
     */
    @NotNull
    default Collection<String> getImportTemplate(@NotNull PsiElement psiElement) {
        return Collections.emptyList();
    }

    /**
     * Checks whether the given element is an import-like template usage.
     */
    default boolean isImportTemplate(@NotNull PsiElement psiElement) {
        return false;
    }

    /**
     * Returns template names from a from-like usage.
     */
    @NotNull
    default Collection<String> getFromTemplate(@NotNull PsiElement psiElement) {
        return Collections.emptyList();
    }

    /**
     * Checks whether the given element is a from-like template usage.
     */
    default boolean isFromTemplate(@NotNull PsiElement psiElement) {
        return false;
    }

    /**
     * Returns template names from a source-like usage.
     */
    @NotNull
    default Collection<String> getSourceTemplate(@NotNull PsiElement psiElement) {
        return Collections.emptyList();
    }

    /**
     * Checks whether the given element is a source-like template usage.
     */
    default boolean isSourceTemplate(@NotNull PsiElement psiElement) {
        return false;
    }

    /**
     * Checks whether the given element is a template-file usage.
     *
     * <p>Implementations should override this when they can identify supported template-file
     * constructs without forcing callers to probe each usage-specific method.</p>
     */
    default boolean isTemplateFileReference(@NotNull PsiElement psiElement) {
        return false;
    }
}
