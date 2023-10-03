package fr.adrienbrault.idea.symfony2plugin.util.dict;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceTag {

    private final HashMap<String, String> attributes = new HashMap<>();

    @NotNull
    private final PhpClass phpClass;

    @NotNull
    private final String tagName;

    @NotNull
    public PhpClass getPhpClass() {
        return phpClass;
    }

    public ServiceTag(@NotNull PhpClass phpClass, @NotNull String tagName) {
        this.phpClass = phpClass;
        this.tagName = tagName;
    }

    public void addAttribute(@NotNull String key, @NotNull String value) {
        this.attributes.put(key, value);
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    @NotNull
    public String getTagName() {
        return tagName;
    }

    @NotNull
    public String toXmlString() {

        List<String> options = new ArrayList<>();
        for (Map.Entry<String, String> entry : this.getAttributes().entrySet()) {
            options.add(String.format("%s=\"%s\"", entry.getKey(), entry.getValue()));
        }

        return String.format("<tag name=\"%s\" %s />", this.getTagName(), StringUtils.join(options, " "));
    }

    @NotNull
    public String toYamlString() {

        List<String> attrs = new ArrayList<>();
        attrs.add(String.format("name: %s", this.getTagName()));

        for (Map.Entry<String, String> entry : this.getAttributes().entrySet()) {
            attrs.add(String.format("%s: %s", entry.getKey(), entry.getValue()));
        }

        return String.format("- { %s }", StringUtils.join(attrs, ", "));
    }

}
