package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Consumer;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigFileUsage;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TemplateInclude;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Isolates rare extension-provided Twig template usages from the built-in Twig PSI paths.
 */
public final class TwigExternalTemplateUsageUtil {

    private TwigExternalTemplateUsageUtil() {
    }

    public enum UsageKind {
        EXTENDS, INCLUDE, EMBED, IMPORT, FROM, SOURCE
    }

    public record Usage(@NotNull PsiElement sourceElement, @NotNull String templateName, @NotNull TemplateInclude.TYPE type) {
    }

    public static void visitTemplateUsages(@NotNull PsiElement psiElement, @NotNull Consumer<Usage> consumer) {
        for (TwigFileUsage extension : TwigUtil.TWIG_FILE_USAGE_EXTENSIONS.getExtensions()) {
            collect(psiElement, extension::isIncludeTemplate, extension::getIncludeTemplate, TemplateInclude.TYPE.INCLUDE, consumer);
            collect(psiElement, extension::isEmbedTemplate, extension::getEmbedTemplate, TemplateInclude.TYPE.EMBED, consumer);
            collect(psiElement, extension::isImportTemplate, extension::getImportTemplate, TemplateInclude.TYPE.IMPORT, consumer);
            collect(psiElement, extension::isFromTemplate, extension::getFromTemplate, TemplateInclude.TYPE.FROM, consumer);
            collect(psiElement, extension::isSourceTemplate, extension::getSourceTemplate, TemplateInclude.TYPE.INCLUDE_FUNCTION, consumer);
        }
    }

    public static @Nullable UsageKind findUsageKind(@NotNull PsiElement psiElement) {
        PsiElement candidate = getCandidateElement(psiElement);
        if (candidate == null) {
            return null;
        }

        for (TwigFileUsage extension : TwigUtil.TWIG_FILE_USAGE_EXTENSIONS.getExtensions()) {
            UsageKind usageKind = getUsageKind(extension, candidate);
            if (usageKind != null) {
                return usageKind;
            }
        }

        return null;
    }

    private static void collect(
        @NotNull PsiElement psiElement,
        @NotNull Predicate<PsiElement> supports,
        @NotNull Function<PsiElement, Collection<String>> templates,
        @NotNull TemplateInclude.TYPE type,
        @NotNull Consumer<Usage> consumer
    ) {
        if (!supports.test(psiElement)) {
            return;
        }

        Collection<String> templateNames = templates.apply(psiElement);
        if (templateNames == null) {
            return;
        }

        for (String templateName : templateNames) {
            if (templateName != null && !templateName.isBlank()) {
                consumer.consume(new Usage(psiElement, templateName, type));
            }
        }
    }

    private static @Nullable UsageKind getUsageKind(@NotNull TwigFileUsage extension, @NotNull PsiElement psiElement) {
        if (extension.isExtendsTemplate(psiElement)) {
            return UsageKind.EXTENDS;
        }

        if (extension.isIncludeTemplate(psiElement)) {
            return UsageKind.INCLUDE;
        }

        if (extension.isEmbedTemplate(psiElement)) {
            return UsageKind.EMBED;
        }

        if (extension.isImportTemplate(psiElement)) {
            return UsageKind.IMPORT;
        }

        if (extension.isFromTemplate(psiElement)) {
            return UsageKind.FROM;
        }

        if (extension.isSourceTemplate(psiElement)) {
            return UsageKind.SOURCE;
        }

        return null;
    }

    private static @Nullable PsiElement getCandidateElement(@NotNull PsiElement psiElement) {
        if (isTwigTag(psiElement)) {
            return psiElement;
        }

        return PsiTreeUtil.findFirstParent(psiElement, true, TwigExternalTemplateUsageUtil::isTwigTag);
    }

    private static boolean isTwigTag(@NotNull PsiElement psiElement) {
        return psiElement.getNode() != null
            && psiElement.getNode().getElementType() == TwigElementTypes.TAG;
    }
}
