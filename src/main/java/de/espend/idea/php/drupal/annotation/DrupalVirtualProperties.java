package de.espend.idea.php.drupal.annotation;

import de.espend.idea.php.annotation.dict.AnnotationPropertyEnum;
import de.espend.idea.php.annotation.extension.PhpAnnotationVirtualProperties;
import de.espend.idea.php.annotation.extension.parameter.AnnotationCompletionProviderParameter;
import de.espend.idea.php.annotation.extension.parameter.AnnotationVirtualPropertyCompletionParameter;
import de.espend.idea.php.annotation.extension.parameter.AnnotationVirtualPropertyTargetsParameter;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DrupalVirtualProperties implements PhpAnnotationVirtualProperties {
    final private static Map<String, Map<String, AnnotationPropertyEnum>> ITEMS = Collections.unmodifiableMap(new HashMap<String, Map<String, AnnotationPropertyEnum>>() {{
        put("Drupal\\Core\\Entity\\Annotation\\ContentEntityType", Collections.unmodifiableMap(new HashMap<String, AnnotationPropertyEnum>() {{
            put("id", AnnotationPropertyEnum.STRING);
            put("label", AnnotationPropertyEnum.STRING);
            put("handlers", AnnotationPropertyEnum.ARRAY);
            put("admin_permission", AnnotationPropertyEnum.STRING);
            put("base_table", AnnotationPropertyEnum.STRING);
            put("data_table", AnnotationPropertyEnum.STRING);
            put("label_callback", AnnotationPropertyEnum.STRING);
            put("translatable", AnnotationPropertyEnum.BOOLEAN);
            put("entity_keys", AnnotationPropertyEnum.ARRAY);
            put("links", AnnotationPropertyEnum.ARRAY);
            put("field_ui_base_route", AnnotationPropertyEnum.STRING);
            put("common_reference_target", AnnotationPropertyEnum.STRING);
        }}));

        put("Drupal\\Core\\Entity\\Annotation\\ConfigEntityType", Collections.unmodifiableMap(new HashMap<String, AnnotationPropertyEnum>() {{
            put("id", AnnotationPropertyEnum.STRING);
            put("label", AnnotationPropertyEnum.STRING);
            put("handlers", AnnotationPropertyEnum.STRING);
            put("entity_keys", AnnotationPropertyEnum.ARRAY);
            put("admin_permission", AnnotationPropertyEnum.STRING);
            put("list_cache_tags", AnnotationPropertyEnum.ARRAY);
            put("config_export", AnnotationPropertyEnum.ARRAY);
        }}));

        put("Drupal\\Component\\Annotation\\Plugin", Collections.unmodifiableMap(new HashMap<String, AnnotationPropertyEnum>() {{
            put("id", AnnotationPropertyEnum.STRING);
            put("title", AnnotationPropertyEnum.STRING);
            put("description", AnnotationPropertyEnum.STRING);
        }}));
    }});

    @Override
    public void addCompletions(@NotNull AnnotationVirtualPropertyCompletionParameter virtualPropertyParameter, @NotNull AnnotationCompletionProviderParameter parameter) {
        String fqn = StringUtils.stripStart(virtualPropertyParameter.getPhpClass().getFQN(), "\\");
        if(!ITEMS.containsKey(fqn)) {
            return;
        }

        for (Map.Entry<String, AnnotationPropertyEnum> item : ITEMS.get(fqn).entrySet()) {
            virtualPropertyParameter.addLookupElement(item.getKey(), item.getValue());
        }
    }

    @Override
    public void getTargets(@NotNull AnnotationVirtualPropertyTargetsParameter parameter) {
        String fqn = StringUtils.stripStart(parameter.getPhpClass().getFQN(), "\\");
        if(!ITEMS.containsKey(fqn)) {
            return;
        }

        if(ITEMS.containsKey(fqn) && ITEMS.get(fqn).containsKey(parameter.getProperty())) {
            parameter.addTarget(parameter.getPhpClass());
        }
    }
}
