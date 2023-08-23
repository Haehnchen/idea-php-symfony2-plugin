package fr.adrienbrault.idea.symfony2plugin.profiler.dict;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record MailMessage(@NotNull String message, @NotNull String title, @NotNull String format, @NotNull String panel) {
}
