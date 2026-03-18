package fr.adrienbrault.idea.symfony2plugin.assistant.review;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Persists a {@link ReviewSession} and optional agent prompt as a Markdown file
 * under {@code {projectRoot}/.review/}.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ReviewMarkdownWriter {

    public static Path write(@NotNull Project project, @NotNull ReviewSession session) throws IOException {
        Path reviewDir = reviewDir(project);
        Files.createDirectories(reviewDir);
        Path file = reviewDir.resolve(session.getId() + ".md");
        Files.writeString(file, buildMarkdown(project, session),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return file;
    }

    public static void appendPrompt(
            @NotNull Project project,
            @NotNull ReviewSession session,
            @NotNull String prompt
    ) throws IOException {
        Path file = reviewDir(project).resolve(session.getId() + ".md");
        if (!Files.exists(file)) {
            write(project, session);
        }
        String section = "\n## Agent Prompt\n\n```\n" + prompt + "\n```\n";
        Files.writeString(file, section, StandardOpenOption.APPEND);
    }

    private static String buildMarkdown(@NotNull Project project, @NotNull ReviewSession session) {
        String basePath = project.getBasePath();
        StringBuilder sb = new StringBuilder();
        sb.append("# Review Session: ").append(session.getTargetDescription()).append("\n");
        sb.append("**Date:** ").append(session.getCreatedAt()).append("\n");
        sb.append("**Status:** pending\n\n## Comments\n\n");

        for (ReviewComment comment : session.getComments()) {
            String path = basePath != null
                    ? comment.getFilePath().replace(basePath + "/", "")
                    : comment.getFilePath();
            sb.append("### ").append(path).append(":").append(comment.getLineNumber() + 1).append("\n");
            sb.append("> ").append(comment.getText().replace("\n", "\n> ")).append("\n\n---\n\n");
        }
        return sb.toString();
    }

    private static Path reviewDir(@NotNull Project project) {
        return Path.of(project.getBasePath() != null ? project.getBasePath() : ".", ".review");
    }
}
