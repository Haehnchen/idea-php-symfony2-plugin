package fr.adrienbrault.idea.symfonyplugin.profiler.collector;

import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class LocalDefaultDataCollector implements DefaultDataCollectorInterface {
    @Nullable
    private final String contents;

    public LocalDefaultDataCollector(@Nullable String contents) {
        this.contents = contents;
    }

    @Nullable
    public String getController() {
        return this.getStringValue(this.contents, "_controller\";s:(\\d+):");
    }

    @Nullable
    public String getRoute() {
        return this.getStringValue(this.contents, "_route\";s:(\\d+):");
    }

    @Nullable
    public String getTemplate() {
        return this.pregMatch(this.contents, "\"template.twig \\(([^\"]*\\.html\\.\\w{2,4})\\)\"");
    }

    @Nullable
    private String pregMatch(@Nullable String content, @RegExp String regular) {
        if(content == null) {
            return null;
        }

        Matcher matcher = Pattern.compile(regular, Pattern.MULTILINE).matcher(content);
        if(matcher.find()){
            return matcher.group(1);
        }

        return null;
    }

    @Nullable
    private String getStringValue(@Nullable String content, @RegExp String regular) {
        if(content == null) {
            return null;
        }

        Matcher matcher = Pattern.compile(regular, Pattern.MULTILINE).matcher(content);
        if(matcher.find()){
            String domain = matcher.group(1);

            int start = matcher.end();
            int end = start + Integer.parseInt(domain);

            return content.substring(start + 1, end + 1);
        }

        return null;
    }
}
