package fr.adrienbrault.idea.symfony2plugin.profiler.collector;

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
        if (this.contents == null) {
            return null;
        }

        // try to find template ordered loading list from "Rendered Templates", to find main template entrypoint
        Matcher matcher = Pattern.compile("\"template_paths\"[\\w;:{]+\"([^\"]+)\"").matcher(this.contents);
        while(matcher.find()){
            int groupStart = matcher.start();
            int i = this.contents.lastIndexOf(Character.toString('\0'), groupStart);
            if (i > 0) {
                String substring = this.contents.substring(i, groupStart);

                // try to find parent scope, to reduce false positive matches
                if (substring.contains("Twig\\Profiler\\Profile")) {
                    String group = matcher.group(1);
                    if (!group.isBlank() && !group.startsWith("@WebProfiler")) {
                        return group;
                    }
                }
            }
        }

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
