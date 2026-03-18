package fr.adrienbrault.idea.symfony2plugin.assistant.review;

import com.intellij.openapi.editor.ComponentInlayAlignment;
import com.intellij.openapi.editor.ComponentInlayKt;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.InlayProperties;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates and removes inline block inlays containing {@link ReviewCommentPanel}.
 * After submitting a comment the panel stays open (text is cleared) so the user
 * can add multiple comments before generating a prompt.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@SuppressWarnings("UnstableApiUsage")
public class ReviewInlayController {

    /** key: identityHashCode(editor) + ":" + lineIndex */
    private static final Map<String, Inlay<?>> ACTIVE_INLAYS = new HashMap<>();

    public static void toggleCommentPanel(
            @NotNull EditorEx editor,
            int lineIndex,
            @NotNull ReviewSessionManager manager
    ) {
        String key = key(editor, lineIndex);
        Inlay<?> existing = ACTIVE_INLAYS.remove(key);
        if (existing != null) {
            Disposer.dispose(existing);
            return;
        }
        openCommentPanel(editor, lineIndex, manager);
    }

    public static boolean hasActivePanel(@NotNull EditorEx editor, int lineIndex) {
        return ACTIVE_INLAYS.containsKey(key(editor, lineIndex));
    }

    private static void openCommentPanel(
            @NotNull EditorEx editor,
            int lineIndex,
            @NotNull ReviewSessionManager manager
    ) {
        // Panel lambdas: submit saves comment & clears text (panel stays open);
        // cancel/close removes the inlay entirely.
        ReviewCommentPanel[] panelRef = new ReviewCommentPanel[1];

        ReviewCommentPanel panel = new ReviewCommentPanel(
                text -> {
                    VirtualFile file = editor.getVirtualFile();
                    String filePath = file != null ? file.getPath() : "unknown";
                    manager.addComment(new ReviewComment(filePath, lineIndex, text));
                    // Keep panel open – clear text so the user can add another comment
                    if (panelRef[0] != null) panelRef[0].clearAndFocus();
                },
                () -> closePanel(editor, lineIndex)
        );
        panelRef[0] = panel;

        int offset = editor.getDocument().getLineEndOffset(lineIndex);
        InlayProperties props = new InlayProperties()
                .relatesToPrecedingText(true)
                .priority(0);

        Inlay<?> inlay = ComponentInlayKt.addComponentInlay(
                editor, offset, props, panel, ComponentInlayAlignment.FIT_VIEWPORT_WIDTH
        );

        if (inlay != null) {
            ACTIVE_INLAYS.put(key(editor, lineIndex), inlay);
            Disposer.register(inlay, () -> ACTIVE_INLAYS.remove(key(editor, lineIndex)));
            SwingUtilities.invokeLater(panel::requestFocusInTextField);
        }
    }

    public static void closeAllPanels(@NotNull EditorEx editor) {
        String prefix = System.identityHashCode(editor) + ":";
        ACTIVE_INLAYS.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(prefix)) {
                Disposer.dispose(entry.getValue());
                return true;
            }
            return false;
        });
    }

    private static void closePanel(@NotNull EditorEx editor, int lineIndex) {
        Inlay<?> inlay = ACTIVE_INLAYS.remove(key(editor, lineIndex));
        if (inlay != null) Disposer.dispose(inlay);
    }

    private static String key(@NotNull EditorEx editor, int line) {
        return System.identityHashCode(editor) + ":" + line;
    }
}
