package fr.adrienbrault.idea.symfony2plugin.profiler.dict;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MailMessage {

    @NotNull
    private final String message;

    @NotNull
    private final String title;

    @NotNull
    private final String format;

    public MailMessage(@NotNull String message, @NotNull String title, @NotNull String format) {
        this.message = message;
        this.title = title;
        this.format = format;
    }

    @NotNull
    public String getMessage() {
        return message;
    }

    @NotNull
    public String getTitle() {
        return title;
    }

    @NotNull
    public String getFormat() {
        return format;
    }

}
