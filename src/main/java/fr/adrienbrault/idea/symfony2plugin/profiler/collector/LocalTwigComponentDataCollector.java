package fr.adrienbrault.idea.symfony2plugin.profiler.collector;

import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerTwigComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalTwigComponentDataCollector implements TwigComponentDataCollectorInterface {
    private static final String TWIG_COMPONENT_COLLECTOR_KEY = "s:14:\"twig_component\"";
    private static final Pattern SERIALIZED_ARRAY_PREFIX = Pattern.compile("a:\\d+:\\{");
    private static final Pattern SERIALIZED_STRING_PREFIX = Pattern.compile("s:(\\d+):\"");

    @Nullable
    private final String contents;

    public LocalTwigComponentDataCollector(@Nullable String contents) {
        this.contents = contents;
    }

    @Override
    public @NotNull Collection<ProfilerTwigComponent> getComponents() {
        String collectorContent = getTwigComponentCollectorContent();
        if (collectorContent == null) {
            return Collections.emptyList();
        }

        ComponentsContent componentsContent = findComponentsContent(collectorContent);
        if (componentsContent == null) {
            return Collections.emptyList();
        }

        Collection<ProfilerTwigComponent> components = new ArrayList<>();
        for (String componentContent : getDirectValues(componentsContent.componentsContent())) {
            componentContent = resolveVarDumperReference(componentContent, componentsContent.dataContent());
            ComponentFields componentFields = parseComponentFields(componentContent);
            if (!componentFields.isComponent()) {
                continue;
            }

            components.add(new ProfilerTwigComponent(
                componentFields.name,
                componentFields.className,
                componentFields.template,
                componentFields.renderCount != null ? componentFields.renderCount : 0
            ));
        }

        return components;
    }

    @Nullable
    private static ComponentsContent findComponentsContent(@NotNull String collectorContent) {
        String dataObjectContent = findDirectValueByKey(collectorContent, "data");
        if (dataObjectContent == null) {
            return null;
        }

        String dataContent = findDirectValueByKey(dataObjectContent, "data");
        if (dataContent == null) {
            return null;
        }

        for (String dataValue : getDirectValues(dataContent)) {
            String componentsReference = findDirectValueByKey(dataValue, "components");
            if (componentsReference == null) {
                continue;
            }

            return new ComponentsContent(resolveVarDumperReference(componentsReference, dataContent), dataContent);
        }

        return null;
    }

    @Nullable
    private String getTwigComponentCollectorContent() {
        if (contents == null || !contents.contains(TWIG_COMPONENT_COLLECTOR_KEY)) {
            return null;
        }

        int collectorIndex = contents.indexOf(TWIG_COMPONENT_COLLECTOR_KEY);
        int valueStart = contents.indexOf(';', collectorIndex + TWIG_COMPONENT_COLLECTOR_KEY.length());
        if (valueStart < 0) {
            return contents.substring(collectorIndex);
        }

        int valueEnd = findSerializedValueEnd(contents, valueStart + 1);
        return valueEnd > valueStart ? contents.substring(valueStart + 1, valueEnd) : contents.substring(collectorIndex);
    }

    private static int findSerializedValueEnd(@NotNull String content, int start) {
        int depth = 0;
        boolean hasCompoundValue = false;

        for (int i = start; i < content.length(); i++) {
            int stringEnd = findSerializedStringEnd(content, i);
            if (stringEnd > i) {
                i = stringEnd - 1;
                continue;
            }

            char c = content.charAt(i);
            if (c == '{') {
                depth++;
                hasCompoundValue = true;
            } else if (c == '}') {
                depth--;
                if (hasCompoundValue && depth == 0) {
                    return i + 1;
                }
            } else if (!hasCompoundValue && c == ';') {
                return i + 1;
            }
        }

        return -1;
    }

    @Nullable
    private static SerializedValue readSerializedValue(@NotNull String content, int start) {
        if (content.startsWith("N;", start)) {
            return new SerializedValue(null, null, start + 2);
        }

        SerializedValue stringValue = readSerializedString(content, start);
        if (stringValue != null) {
            return stringValue;
        }

        if (content.startsWith("i:", start)) {
            int end = content.indexOf(';', start + 2);
            if (end > start) {
                return new SerializedValue(null, parseInteger(content.substring(start + 2, end)), end + 1);
            }
        }

        int valueEnd = findSerializedValueEnd(content, start);
        return valueEnd > start ? new SerializedValue(null, null, valueEnd) : null;
    }

    @Nullable
    private static SerializedValue readSerializedString(@NotNull String content, int start) {
        Matcher matcher = SERIALIZED_STRING_PREFIX.matcher(content);
        matcher.region(start, content.length());
        if (!matcher.lookingAt()) {
            return null;
        }

        int length;
        try {
            length = Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }

        int stringEnd = matcher.end() + length;
        if (stringEnd + 2 <= content.length() && content.startsWith("\";", stringEnd)) {
            return new SerializedValue(content.substring(matcher.end(), stringEnd), null, stringEnd + 2);
        }

        int fallbackStringEnd = content.indexOf("\";", matcher.end());
        if (fallbackStringEnd > matcher.end()) {
            return new SerializedValue(content.substring(matcher.end(), fallbackStringEnd), null, fallbackStringEnd + 2);
        }

        return null;
    }

    private static int findSerializedStringEnd(@NotNull String content, int start) {
        SerializedValue value = readSerializedString(content, start);
        return value != null ? value.end : -1;
    }

    @Nullable
    private static String findDirectValueByKey(@NotNull String serializedContent, @NotNull String expectedKey) {
        for (SerializedPair pair : getDirectPairs(serializedContent)) {
            if (pair.key.stringValue != null && isPropertyName(pair.key.stringValue, expectedKey)) {
                return pair.rawValue;
            }
        }

        return null;
    }

    @Nullable
    private static String findDirectValueByIntegerKey(@NotNull String serializedContent, int expectedKey) {
        for (SerializedPair pair : getDirectPairs(serializedContent)) {
            if (pair.key.integerValue != null && pair.key.integerValue == expectedKey) {
                return pair.rawValue;
            }
        }

        return null;
    }

    @NotNull
    private static String resolveVarDumperReference(@NotNull String serializedContent, @NotNull String dataContent) {
        String referencedIndex = findDirectValueByIntegerKey(serializedContent, 1);
        if (referencedIndex == null) {
            return serializedContent;
        }

        SerializedValue referenceValue = readSerializedValue(referencedIndex, 0);
        if (referenceValue == null || referenceValue.integerValue == null) {
            return serializedContent;
        }

        String resolvedContent = findDirectValueByIntegerKey(dataContent, referenceValue.integerValue);
        return resolvedContent != null ? resolvedContent : serializedContent;
    }

    @NotNull
    private static Collection<String> getDirectValues(@NotNull String serializedContent) {
        Collection<String> values = new ArrayList<>();
        for (SerializedPair pair : getDirectPairs(serializedContent)) {
            values.add(pair.rawValue);
        }

        return values;
    }

    @NotNull
    private static Collection<SerializedPair> getDirectPairs(@NotNull String serializedContent) {
        Collection<SerializedPair> pairs = new ArrayList<>();

        Matcher arrayMatcher = SERIALIZED_ARRAY_PREFIX.matcher(serializedContent);
        if (!arrayMatcher.lookingAt() && !serializedContent.startsWith("O:")) {
            return pairs;
        }

        int bodyStart = serializedContent.indexOf('{');
        if (bodyStart < 0) {
            return pairs;
        }

        int offset = bodyStart + 1;
        while (offset < serializedContent.length() && serializedContent.charAt(offset) != '}') {
            SerializedValue key = readSerializedValue(serializedContent, offset);
            if (key == null) {
                break;
            }

            offset = key.end;

            int valueStart = offset;
            SerializedValue value = readSerializedValue(serializedContent, valueStart);
            if (value == null) {
                break;
            }

            offset = value.end;
            pairs.add(new SerializedPair(key, value, serializedContent.substring(valueStart, value.end)));
        }

        return pairs;
    }

    private static boolean isPropertyName(@NotNull String key, @NotNull String expectedKey) {
        int nullByte = key.lastIndexOf('\0');
        String normalizedKey = nullByte >= 0 ? key.substring(nullByte + 1) : key;
        return expectedKey.equals(normalizedKey);
    }

    @NotNull
    private static ComponentFields parseComponentFields(@NotNull String componentArrayContent) {
        ComponentFields fields = new ComponentFields();

        for (SerializedPair pair : getDirectPairs(componentArrayContent)) {
            if (pair.key.stringValue == null) {
                continue;
            }

            switch (pair.key.stringValue) {
                case "name" -> fields.name = pair.value.stringValue;
                case "class" -> fields.className = normalizePhpClassName(pair.value.stringValue);
                case "template" -> fields.template = pair.value.stringValue;
                case "render_count" -> fields.renderCount = pair.value.integerValue;
            }
        }

        return fields;
    }

    @Nullable
    private static String normalizePhpClassName(@Nullable String className) {
        if (className == null || className.isBlank()) {
            return null;
        }

        return className.startsWith("\\") ? className : "\\" + className;
    }

    @Nullable
    private static Integer parseInteger(@Nullable String value) {
        if (value == null) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static class ComponentFields {
        private String name;
        private String className;
        private String template;
        private Integer renderCount;

        private boolean isComponent() {
            return name != null && (className != null || template != null || renderCount != null);
        }
    }

    private record SerializedValue(@Nullable String stringValue, @Nullable Integer integerValue, int end) {
    }

    private record SerializedPair(@NotNull SerializedValue key, @NotNull SerializedValue value, @NotNull String rawValue) {
    }

    private record ComponentsContent(@NotNull String componentsContent, @NotNull String dataContent) {
    }
}
