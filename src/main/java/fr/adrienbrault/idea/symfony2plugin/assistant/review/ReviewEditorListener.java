package fr.adrienbrault.idea.symfony2plugin.assistant.review;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;

/**
 * Installs {@link ReviewGutterRenderer} on every editor opened while a review session is active.
 * Also activates existing editors when a session starts.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ReviewEditorListener implements EditorFactoryListener {

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        Project project = editor.getProject();
        if (project == null || !(editor instanceof EditorEx editorEx)) return;

        ReviewSessionManager manager = ReviewSessionManager.getInstance(project);
        if (manager.getActiveSession() == null) return;

        ReviewGutterRenderer.install(editorEx, manager, Disposer.newDisposable("ReviewGutter-" + System.identityHashCode(editor)));
    }

    /**
     * Called by {@link fr.adrienbrault.idea.symfony2plugin.assistant.review.action.StartReviewAction}
     * to activate gutter rendering on all currently open editors for the project.
     */
    public static void activateForAllEditors(@NotNull Project project) {
        ReviewSessionManager manager = ReviewSessionManager.getInstance(project);
        if (manager.getActiveSession() == null) return;

        for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
            if (editor.getProject() != project) continue;
            if (!(editor instanceof EditorEx editorEx)) continue;

            ReviewGutterRenderer.install(
                    editorEx,
                    manager,
                    Disposer.newDisposable("ReviewGutter-" + System.identityHashCode(editor))
            );
        }
    }
}
