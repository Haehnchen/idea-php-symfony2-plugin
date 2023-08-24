package fr.adrienbrault.idea.symfony2plugin.profiler.collector;

import kotlin.Pair;
import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
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

    @Override
    public @NotNull Collection<String> getFormTypes() {
        if (this.contents == null) {
            return Collections.emptyList();
        }

        List<Pair<String, String>> types = new ArrayList<>();

        // try to find root "type_class" from "FormDataCollector"
        Matcher matcher = Pattern.compile("\\\\FormDataCollector\"").matcher(this.contents);
        while (matcher.find()){
            String substring = this.contents.substring(matcher.start());

            int i1 = substring.indexOf("\"forms\"");
            if (i1 > 0) {
                String forms = substring.substring(i1);
                int nullEndingCollector = forms.indexOf(Character.toString('\0'));
                if (nullEndingCollector > 0) {
                    String formDataCollectorContent = forms.substring(0, nullEndingCollector);

                    Matcher matcherTypeClass = Pattern.compile("type_class\"[\\w;:\\\\\"{]+\"value\"[\\w;:]+\"([^\"]+)\"").matcher(formDataCollectorContent);
                    while (matcherTypeClass.find()){
                        String formTyp = matcherTypeClass.group(1);
                        String formId = null;

                        int formContent = formDataCollectorContent.lastIndexOf("\"id\"", matcherTypeClass.start());
                        if (formContent > 0) {
                            Matcher matcherId = Pattern.compile("\"id\";s:\\d+:\"([^\"]+)\"").matcher(formDataCollectorContent.substring(formContent, matcherTypeClass.start()));
                            if (matcherId.find()){
                                formId = matcherId.group(1);
                            }
                        }

                        types.add(new Pair<>(formId, formTyp));
                    }
                }
            }
        }

        if (types.isEmpty()) {
            return Collections.emptyList();
        }

        // use the first match only; as want only main forms, but getting all children
        //  - to resolve: "forms_by_hash" would have a list of root forms
        // - "FormType" which don't provide a valuable target can be removed (no type class)
        String second = types.get(0).getSecond();
        if (second.equalsIgnoreCase("Symfony\\Component\\Form\\Extension\\Core\\Type\\FormType")) {
            return Collections.emptyList();
        }

        return Collections.singletonList(second);
    }
}
