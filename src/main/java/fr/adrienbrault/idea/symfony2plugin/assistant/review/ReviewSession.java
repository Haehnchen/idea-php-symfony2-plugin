package fr.adrienbrault.idea.symfony2plugin.assistant.review;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An active review session collecting comments for an agent run.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ReviewSession {

    private final String id;
    private final String targetDescription;
    private final Instant createdAt;
    private final List<ReviewComment> comments = new ArrayList<>();

    public ReviewSession(String id, String targetDescription) {
        this.id = id;
        this.targetDescription = targetDescription;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getTargetDescription() {
        return targetDescription;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void addComment(ReviewComment comment) {
        comments.add(comment);
    }

    public List<ReviewComment> getComments() {
        return Collections.unmodifiableList(comments);
    }
}
