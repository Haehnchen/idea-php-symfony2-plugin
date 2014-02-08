package fr.adrienbrault.idea.symfony2plugin.profiler.dict;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultDataCollector implements CollectorInterface {

    private ProfilerRequest profilerRequest;

    @Nullable
    public String getController() {
        return this.getStringValue(this.profilerRequest.getContent(), "_controller\";s:(\\d+):");
    }

    @Nullable
    public String getRoute() {
        return this.getStringValue(this.profilerRequest.getContent(), "_route\";s:(\\d+):");
    }

    @Nullable
    public String getStatusCode() {
        return this.pregMatch(this.profilerRequest.getContent(), "status_code\";i:(\\d+);");
    }

    @Nullable
    public String getTemplate() {
        return this.pregMatch(this.profilerRequest.getContent(), "\"template.twig \\(([^\"]*\\.html\\.\\w{2,4})\\)\"");
    }
    @Nullable
    protected String pregMatch(@Nullable String content, String regular) {
        if(content == null) {
            return null;
        }

        Matcher matcher = Pattern.compile(regular, Pattern.MULTILINE).matcher(content);
        while(matcher.find()){
            return matcher.group(1);
        }

        return null;

    }

    @Nullable
    protected String getStringValue(@Nullable String content, String regular) {
        if(content == null) {
            return null;
        }

        Matcher matcher = Pattern.compile(regular, Pattern.MULTILINE).matcher(content);
        while(matcher.find()){
            String domain = matcher.group(1);
            //String array_strings = matcher.group(2);

            int start = matcher.end();
            int end = start + Integer.parseInt(domain);

            return content.substring(start + 1, end + 1);

        }

        return null;

    }

    @Override
    public void setProfilerRequest(ProfilerRequest profilerRequest) {
        this.profilerRequest = profilerRequest;
    }
}
