package fr.adrienbrault.idea.symfony2plugin.assistant.review.action;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import fr.adrienbrault.idea.symfony2plugin.assistant.review.ReviewComment;
import fr.adrienbrault.idea.symfony2plugin.assistant.review.ReviewMarkdownWriter;
import fr.adrienbrault.idea.symfony2plugin.assistant.review.ReviewSession;
import fr.adrienbrault.idea.symfony2plugin.assistant.review.ReviewSessionManager;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Generates an agent prompt from all collected review comments, copies it to the clipboard,
 * and saves the session + prompt as a Markdown file under {@code .review/}.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class GenerateAgentPromptAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        ReviewSession session = ReviewSessionManager.getInstance(project).getActiveSession();
        if (session == null || session.getComments().isEmpty()) {
            Messages.showInfoMessage(project, "No review comments yet. Start a review and add comments first.", "Internal Review");
            return;
        }

        String prompt = buildPrompt(session);
        CopyPasteManager.getInstance().setContents(new StringSelection(prompt));

        try {
            Path file = ReviewMarkdownWriter.write(project, session);
            ReviewMarkdownWriter.appendPrompt(project, session, prompt);

            Notifications.Bus.notify(new Notification(
                    "Symfony Plugin",
                    "Agent Prompt Generated",
                    "Prompt with " + session.getComments().size() + " comment(s) copied to clipboard.\n" +
                            "Saved to: " + file.getFileName(),
                    NotificationType.INFORMATION
            ), project);
        } catch (IOException ex) {
            Notifications.Bus.notify(new Notification(
                    "Symfony Plugin", "Review Write Error",
                    ex.getMessage(), NotificationType.WARNING
            ), project);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        ReviewSession session = ReviewSessionManager.getInstance(project).getActiveSession();
        e.getPresentation().setEnabled(session != null && !session.getComments().isEmpty());
    }

    private static String buildPrompt(@NotNull ReviewSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("Please address the following code review comments from session \"")
                .append(session.getTargetDescription()).append("\":\n\n");

        for (ReviewComment comment : session.getComments()) {
            sb.append("**File:** `").append(comment.getFilePath()).append("`");
            sb.append(" **Line:** ").append(comment.getLineNumber() + 1).append("\n");
            sb.append("**Comment:** ").append(comment.getText()).append("\n\n");
        }

        sb.append("For each comment:\n");
        sb.append("1. Understand the concern raised\n");
        sb.append("2. Make the minimal necessary change to address it\n");
        sb.append("3. Explain what you changed and why\n");
        return sb.toString();
    }
}
