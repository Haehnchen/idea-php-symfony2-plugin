package fr.adrienbrault.idea.symfony2plugin.translation.collector;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface YamlTranslationCollector {
    boolean collect(@NotNull String keyName, YAMLKeyValue yamlKeyValue);
}
