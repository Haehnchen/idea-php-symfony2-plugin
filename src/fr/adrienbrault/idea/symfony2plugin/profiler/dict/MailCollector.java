package fr.adrienbrault.idea.symfony2plugin.profiler.dict;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MailCollector implements CollectorInterface {

    private ProfilerRequest profilerRequest;

    private ArrayList<MailMessage> messages = new ArrayList<MailMessage>();

    synchronized public ArrayList<MailMessage> getMessages() {

        String content = this.profilerRequest.getContent();
        if(content == null) {
            return this.messages;
        }

        String messages = this.findTwice(content, "MessageDataCollector\":(\\d+):");

        Matcher matcher = Pattern.compile("\"\\x00Swift_Mime_SimpleMimeEntity\\x00_body\";s:(\\d+):\"", Pattern.MULTILINE).matcher(messages);

        while(matcher.find()){
            String domain = matcher.group(1);
            //String array_strings = matcher.group(2);

            int start = matcher.end();
            int end = start + Integer.parseInt(domain);

            //System.out.println(content.substring(start, end));
            this.messages.add(new MailMessage(messages.substring(start, end), "aa", "aa"));

            //Matcher match_strings = Pattern.compile("'(.*?)'\\s=>\\s'.*?'", Pattern.MULTILINE).matcher(array_strings);
            //while(match_strings.find()){
            // string_map.addString(domain, match_strings.group(1));
            //}

        }

        return this.messages;
    }

    @Nullable
    protected String findTwice(String content, String regular) {
        Matcher matcher = Pattern.compile(regular, Pattern.MULTILINE).matcher(content);
        while(matcher.find()){
            String domain = matcher.group(1);
            //String array_strings = matcher.group(2);

            int start = matcher.end();
            int end = start + Integer.parseInt(domain);

            return content.substring(start, end);

        }

        return null;

    }

    @Override
    public void setProfilerRequest(ProfilerRequest profilerRequest) {
        this.profilerRequest = profilerRequest;
    }
}
