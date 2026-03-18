package fr.adrienbrault.idea.symfony2plugin.assistant.review;

import java.time.Instant;

/**
 * A single inline review comment attached to a file/line.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ReviewComment {

    private final String filePath;
    private final int lineNumber;
    private final String text;
    private final Instant createdAt;

    public ReviewComment(String filePath, int lineNumber, String text) {
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.text = text;
        this.createdAt = Instant.now();
    }

    public String getFilePath() {
        return filePath;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getText() {
        return text;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
