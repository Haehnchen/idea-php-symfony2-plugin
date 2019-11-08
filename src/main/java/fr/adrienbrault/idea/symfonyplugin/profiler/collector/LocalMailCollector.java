package fr.adrienbrault.idea.symfonyplugin.profiler.collector;

import fr.adrienbrault.idea.symfonyplugin.profiler.dict.MailMessage;
import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
        String messages = this.findTwice(this.contents, "MessageDataCollector\":(\\d+):");
        if(messages == null) {
            return Collections.emptyList();
        }

        Matcher matcher = Pattern.compile("\"\\x00Swift_Mime_SimpleMimeEntity\\x00_body\";s:(\\d+):\"", Pattern.MULTILINE).matcher(messages);

        Collection<MailMessage> mails = new ArrayList<>();
        while(matcher.find()){
            String domain = matcher.group(1);
            //String array_strings = matcher.group(2);

            int start = matcher.end();
            int end = start + Integer.parseInt(domain);

            //System.out.println(content.substring(start, end));
            mails.add(new MailMessage(messages.substring(start, end), "aa", "aa"));

            //Matcher match_strings = Pattern.compile("'(.*?)'\\s=>\\s'.*?'", Pattern.MULTILINE).matcher(array_strings);
            //while(match_strings.find()){
            // string_map.addString(domain, match_strings.group(1));
            //}

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
