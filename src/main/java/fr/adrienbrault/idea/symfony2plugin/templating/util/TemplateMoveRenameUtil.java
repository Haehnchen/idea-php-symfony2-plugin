package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Shared utilities for updating Twig template name strings during file-move refactoring.
 *
 * Used by {@link fr.adrienbrault.idea.symfony2plugin.templating.TemplateReference} (PHP render calls)
 * and {@link fr.adrienbrault.idea.symfony2plugin.templating.usages.TwigTemplateUsageReference}
 * (Twig include / extends / embed / … tags).
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class TemplateMoveRenameUtil {

    private TemplateMoveRenameUtil() {
    }

    /**
     * Updates the content of a PHP string literal (e.g. a {@code render()} argument) to
     * {@code newTemplateName} using the element's registered {@link com.intellij.psi.ElementManipulator}.
     *
     * @return the (possibly replaced) element, or {@code null} if no manipulator is registered.
     */
    @Nullable
    public static PsiElement applyToStringLiteralElement(@NotNull PsiElement element, @NotNull String newTemplateName) {
        var manipulator = ElementManipulators.getManipulator(element);
        if (manipulator == null) {
            return null;
        }
        return manipulator.handleContentChange(element, manipulator.getRangeInElement(element), newTemplateName);
    }

    /**
     * Replaces the text at {@code rangeInElement} within {@code element} with {@code newText}.
     *
     * <p>Uses {@code element.getTextRange().getStartOffset()} (the true document start of the
     * element, never overridden unlike {@code getTextOffset()}) plus the range's own start to
     * compute a reliable absolute document offset.
     *
     * @return the element itself, or {@code null} if the containing document cannot be resolved.
     */
    @Nullable
    public static PsiElement applyRangeReplacement(
            @NotNull PsiElement element,
            @NotNull TextRange rangeInElement,
            @NotNull String newText) {

        PsiFile containingFile = element.getContainingFile();
        if (containingFile == null) {
            return null;
        }

        Project project = element.getProject();
        Document document = PsiDocumentManager.getInstance(project).getDocument(containingFile);
        if (document == null) {
            return null;
        }

        int absoluteStart = element.getTextRange().getStartOffset() + rangeInElement.getStartOffset();
        document.replaceString(absoluteStart, absoluteStart + rangeInElement.getLength(), newText);
        PsiDocumentManager.getInstance(project).commitDocument(document);

        return element;
    }

    /**
     * Picks the new template name whose namespace prefix best matches the style of {@code oldName}.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code "@App/old.twig"} + {@code ["plain.twig", "@App/new.twig"]} → {@code "@App/new.twig"}
     *   <li>{@code "Bundle:dir:old.twig"} + {@code ["new.twig", "Bundle:dir:new.twig"]} → {@code "Bundle:dir:new.twig"}
     *   <li>No match → first name in {@code newNames}
     * </ul>
     */
    @NotNull
    public static String pickBestTemplateName(@NotNull Collection<String> newNames, @NotNull String oldName) {
        if (newNames.size() == 1) {
            return newNames.iterator().next();
        }

        String oldPrefix = extractNamespacePrefix(oldName);
        for (String name : newNames) {
            if (extractNamespacePrefix(name).equals(oldPrefix)) {
                return name;
            }
        }

        return newNames.iterator().next();
    }

    /**
     * Extracts the namespace prefix from a Twig template name:
     * <ul>
     *   <li>{@code "@App/foo/bar.twig"} → {@code "@App"}
     *   <li>{@code "FooBundle:dir:bar.twig"} → {@code "FooBundle"}
     *   <li>{@code "foo/bar.twig"} → {@code ""}
     * </ul>
     */
    @NotNull
    public static String extractNamespacePrefix(@NotNull String templateName) {
        if (templateName.startsWith("@")) {
            int slash = templateName.indexOf('/');
            return slash > 0 ? templateName.substring(0, slash) : templateName;
        }
        if (templateName.contains(":")) {
            int colon = templateName.indexOf(':');
            return templateName.substring(0, colon);
        }
        return "";
    }

    /**
     * Replaces only the filename portion of a logical Twig template name while keeping its
     * namespace/path style intact.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code "foo/bar.html.twig" + "baz.html.twig" -> "foo/baz.html.twig"}</li>
     *   <li>{@code "@App/foo/bar.html.twig" + "baz.html.twig" -> "@App/foo/baz.html.twig"}</li>
     *   <li>{@code "FooBundle:dir:bar.html.twig" + "baz.html.twig" -> "FooBundle:dir:baz.html.twig"}</li>
     *   <li>{@code "bar.html.twig" + "baz.html.twig" -> "baz.html.twig"}</li>
     * </ul>
     */
    @NotNull
    public static String renameTemplateName(@NotNull String oldTemplateName, @NotNull String newFileName) {
        int slash = oldTemplateName.lastIndexOf('/');
        int colon = oldTemplateName.lastIndexOf(':');
        int separator = Math.max(slash, colon);

        if (separator < 0) {
            return newFileName;
        }

        return oldTemplateName.substring(0, separator + 1) + newFileName;
    }

    /**
     * Computes the new template names for a moved Twig file and delegates to the caller.
     * Returns the best-matching name, or {@code null} when the file falls outside all configured
     * Twig namespaces (i.e. we cannot compute a logical name for the new location).
     */
    @Nullable
    public static String resolveNewTemplateName(@NotNull Project project,
                                                @NotNull VirtualFile newVirtualFile,
                                                @NotNull String oldTemplateName) {
        Collection<String> newNames = TwigUtil.getTemplateNamesForFile(project, newVirtualFile);
        if (newNames.isEmpty()) {
            return null;
        }
        return pickBestTemplateName(newNames, oldTemplateName);
    }

    /**
     * Locates the {@code STRING_TEXT} leaf at the given <em>absolute document offset</em>
     * within {@code containingFile} and replaces its full text with {@code newTemplateName}.
     *
     * <p>Takes a {@link PsiFile} re-fetchable from a {@link VirtualFile} and a pre-computed
     * absolute offset, making it safe to call from {@code MoveFileHandler.retargetUsages()} where
     * PSI elements recorded at {@code findUsages()} time may have been invalidated.
     *
     * @param containingFile  the file that contains the {@code {% extends %}} or similar tag
     * @param absoluteOffset  absolute document offset of the first character of the template name
     * @param newTemplateName the Symfony logical name to write
     * @return {@code true} if the replacement was applied, {@code false} otherwise
     */
    public static boolean applyToTwigFileByOffset(
            @NotNull PsiFile containingFile,
            int absoluteOffset,
            @NotNull String newTemplateName) {

        if (!(containingFile instanceof TwigFile)) {
            return false;
        }

        Project project = containingFile.getProject();
        Document document = PsiDocumentManager.getInstance(project).getDocument(containingFile);
        if (document == null) {
            return false;
        }

        if (absoluteOffset < 0 || absoluteOffset >= document.getTextLength()) {
            return false;
        }

        PsiElement leaf = containingFile.findElementAt(absoluteOffset);

        while (leaf != null
                && !(leaf instanceof LeafPsiElement && ((LeafPsiElement) leaf).getElementType() == TwigTokenTypes.STRING_TEXT)) {
            if (leaf instanceof PsiFile) {
                leaf = null;
                break;
            }
            leaf = leaf.getParent();
        }

        if (leaf == null) {
            return false;
        }

        int leafStart = leaf.getTextOffset();
        document.replaceString(leafStart, leafStart + leaf.getTextLength(), newTemplateName);
        PsiDocumentManager.getInstance(project).commitDocument(document);

        return true;
    }

}
