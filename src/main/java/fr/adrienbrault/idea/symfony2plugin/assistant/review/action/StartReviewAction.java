package fr.adrienbrault.idea.symfony2plugin.assistant.review.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import fr.adrienbrault.idea.symfony2plugin.assistant.review.ReviewEditorListener;
import fr.adrienbrault.idea.symfony2plugin.assistant.review.ReviewSession;
import fr.adrienbrault.idea.symfony2plugin.assistant.review.ReviewSessionManager;
import org.jetbrains.annotations.NotNull;

/**
 * Starts a new internal review session, enabling the gutter "+" icons on all open editors.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class StartReviewAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        ReviewSessionManager manager = ReviewSessionManager.getInstance(project);

        // Ask for an optional description (e.g. "Agent Run #42")
        String description = Messages.showInputDialog(
                project,
                "Review description (e.g. \"Agent Run #42\"):",
                "Start Internal Review",
                null
        );
        if (description == null) return; // user cancelled

        if (description.isBlank()) {
            description = "Internal Review";
        }

        ReviewSession session = manager.startSession(description);

        // Activate gutter icons on all currently open editors
        ReviewEditorListener.activateForAllEditors(project);

        Messages.showInfoMessage(
                project,
                "Review session started: " + session.getId() + "\n" +
                        "Click the \"+\" icon in the gutter to add comments.",
                "Internal Review"
        );
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabled(project != null);
    }
}
