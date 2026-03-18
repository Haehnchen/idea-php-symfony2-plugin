package fr.adrienbrault.idea.symfony2plugin.assistant.review;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Project-level service managing the active review session.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@Service(Service.Level.PROJECT)
public final class ReviewSessionManager {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final AtomicInteger COUNTER = new AtomicInteger(1);

    @Nullable
    private ReviewSession activeSession;

    public static ReviewSessionManager getInstance(@NotNull Project project) {
        return project.getService(ReviewSessionManager.class);
    }

    @NotNull
    public ReviewSession startSession(@NotNull String targetDescription) {
        String id = "review-" + LocalDate.now().format(DATE_FMT) + "-" + String.format("%03d", COUNTER.getAndIncrement());
        activeSession = new ReviewSession(id, targetDescription);
        return activeSession;
    }

    @Nullable
    public ReviewSession getActiveSession() {
        return activeSession;
    }

    public void addComment(@NotNull ReviewComment comment) {
        if (activeSession != null) {
            activeSession.addComment(comment);
        }
    }

    public void endSession() {
        activeSession = null;
    }
}
