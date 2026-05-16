package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ConstantFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.function.Supplier;

/**
 * Centralizes line-marker creation for elements that may be whole files.
 * File markers are anchored to the first non-blank leaf and clipped to one line,
 * so unified diff views do not repeat one file-wide marker in every changed block.
 */
public final class FileLineMarkerUtil {
    private FileLineMarkerUtil() {
    }

    /**
     * Creates a builder-based marker for a file anchor.
     * Example: {@code FileLineMarkerUtil.createLineMarkerInfo(builder, twigFile)}.
     */
    @NotNull
    public static RelatedItemLineMarkerInfo<PsiElement> createLineMarkerInfo(
        @NotNull NavigationGutterIconBuilder<PsiElement> builder,
        @NotNull PsiFile psiFile
    ) {
        LineMarkerAnchor anchor = getFileLineMarkerAnchor(psiFile);
        PsiElement target = anchor.element();
        RelatedItemLineMarkerInfo<PsiElement> markerInfo = builder.createLineMarkerInfo(target);

        TextRange range = anchor.textRange();
        if (range.equals(target.getTextRange())) {
            return markerInfo;
        }

        String tooltip = markerInfo.getLineMarkerTooltip();
        return new RelatedItemLineMarkerInfo<>(
            target,
            range,
            markerInfo.getIcon(),
            tooltip == null ? null : new ConstantFunction<>(tooltip),
            markerInfo.getNavigationHandler(),
            GutterIconRenderer.Alignment.RIGHT,
            markerInfo::createGotoRelatedItems
        );
    }

    /**
     * Creates a direct marker using the same anchoring rules as builder markers.
     * Example: {@code FileLineMarkerUtil.createLineMarkerInfo(target, icon, tooltip, handler, () -> "Go to related files")}.
     */
    @NotNull
    public static LineMarkerInfo<PsiElement> createLineMarkerInfo(
        @NotNull PsiElement lineMarkerTarget,
        @NotNull Icon icon,
        @Nullable String tooltip,
        @Nullable GutterIconNavigationHandler<PsiElement> navigationHandler,
        @NotNull Supplier<String> accessibleNameProvider
    ) {
        LineMarkerAnchor anchor = getLineMarkerAnchor(lineMarkerTarget);

        return new LineMarkerInfo<>(
            anchor.element(),
            anchor.textRange(),
            icon,
            tooltip == null ? null : new ConstantFunction<>(tooltip),
            navigationHandler,
            GutterIconRenderer.Alignment.RIGHT,
            accessibleNameProvider
        );
    }

    @NotNull
    private static LineMarkerAnchor getLineMarkerAnchor(@NotNull PsiElement element) {
        if (element instanceof PsiFile psiFile) {
            return getFileLineMarkerAnchor(psiFile);
        }

        return new LineMarkerAnchor(element, element.getTextRange());
    }

    /**
     * Returns the file anchor used by {@link #createLineMarkerInfo(NavigationGutterIconBuilder, PsiFile)}.
     * Useful in tests that need to assert the exact marker element and range.
     */
    @NotNull
    public static LineMarkerAnchor getFileLineMarkerAnchor(@NotNull PsiFile psiFile) {
        PsiElement target = getFileLineMarkerTarget(psiFile);
        return new LineMarkerAnchor(target, getFileLineMarkerRange(psiFile, target));
    }

    @NotNull
    private static PsiElement getFileLineMarkerTarget(@NotNull PsiFile psiFile) {
        for (PsiElement leaf = PsiTreeUtil.getDeepestFirst(psiFile); leaf != null && leaf != psiFile; leaf = PsiTreeUtil.nextLeaf(leaf)) {
            if (leaf.getTextLength() > 0 && !StringUtil.isEmptyOrSpaces(leaf.getText())) {
                return leaf;
            }
        }

        return psiFile;
    }

    @NotNull
    private static TextRange getFileLineMarkerRange(@NotNull PsiFile psiFile, @NotNull PsiElement target) {
        TextRange targetRange = target.getTextRange();
        int startOffset = targetRange.getStartOffset();
        int endOffset = targetRange.getEndOffset();

        Document document = PsiDocumentManager.getInstance(psiFile.getProject()).getDocument(psiFile);
        if (document == null) {
            return targetRange;
        }

        int textLength = document.getTextLength();
        if (startOffset >= textLength) {
            return TextRange.create(startOffset, startOffset);
        }

        int lineEndOffset = document.getLineEndOffset(document.getLineNumber(startOffset));
        int clippedEndOffset = Math.min(endOffset, lineEndOffset);
        if (clippedEndOffset <= startOffset) {
            clippedEndOffset = Math.min(startOffset + 1, textLength);
        }

        return TextRange.create(startOffset, clippedEndOffset);
    }

    /**
     * The PSI element and text range that should receive the gutter marker.
     */
    public record LineMarkerAnchor(@NotNull PsiElement element, @NotNull TextRange textRange) {
    }
}
