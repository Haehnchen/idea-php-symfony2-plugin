package fr.adrienbrault.idea.symfony2plugin.profiler.dict;

public class MailMessage {


    private final String message;
    private final String title;
    private final String format;

    public MailMessage(String message, String title, String format) {
        this.message = message;
        this.title = title;
        this.format = format;
    }

    public String getMessage() {
        return message;
    }

    public String getTitle() {
        return title;
    }

    public String getFormat() {
        return format;
    }

}
