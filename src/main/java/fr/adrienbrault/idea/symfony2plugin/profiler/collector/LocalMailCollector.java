package fr.adrienbrault.idea.symfony2plugin.profiler.collector;

import fr.adrienbrault.idea.symfony2plugin.profiler.dict.MailMessage;
import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class LocalMailCollector implements MailCollectorInterface {

    @NotNull
    private final String contents;

    public LocalMailCollector(@NotNull String contents) {
        this.contents = contents;
    }

    @NotNull
    public Collection<MailMessage> getMessages() {
        Collection<MailMessage> mails = new ArrayList<>();

        String messages = this.findTwice(this.contents, "MessageDataCollector\":(\\d+):");
        if(messages != null) {
            Matcher matcher = Pattern.compile("\"\\x00Swift_Mime_SimpleMimeEntity\\x00_body\";s:(\\d+):\"", Pattern.MULTILINE).matcher(messages);

            while(matcher.find()){
                String domain = matcher.group(1);

                int start = matcher.end();
                int end = start + Integer.parseInt(domain);

                mails.add(new MailMessage(messages.substring(start, end), "", "", "swiftmailer"));
            }
        }

        // try to find any serialized object inside the mailer class
        Matcher matcher = Pattern.compile("AbstractHeader\\x00name\"[\\w;:]+\"Subject\"").matcher(this.contents);
        Set<String> titles = new HashSet<>();

        while(matcher.find()){
            int end = matcher.end();

            // find ending scope
            int endingHeaderScope = contents.indexOf("}", end);
            if (endingHeaderScope > 0) {
                String subjectHeaderScope = contents.substring(matcher.start(), endingHeaderScope);

                // find the email "Subject" header value
                Matcher matcher2 = Pattern.compile("UnstructuredHeader.*value\"[\\w;:]+\"(.*)\"").matcher(subjectHeaderScope);
                if (matcher2.find()) {
                    String subject = matcher2.group(1);
                    if (!subject.isBlank()) {
                        titles.add(subject);
                    }
                }
            }
        }

        for (String title : titles) {
            mails.add(new MailMessage("", title, "", "mailer"));
        }

        return mails;
    }

    @Nullable
    private String findTwice(@NotNull String content, @RegExp String regular) {
        Matcher matcher = Pattern.compile(regular, Pattern.MULTILINE).matcher(content);
        if(matcher.find()){
            String domain = matcher.group(1);

            int start = matcher.end();
            int end = start + Integer.parseInt(domain);

            return content.substring(start, end);
        }

        return null;
    }
}
