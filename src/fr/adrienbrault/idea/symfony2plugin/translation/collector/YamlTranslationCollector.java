package fr.adrienbrault.idea.symfony2plugin.translation.collector;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;

public interface YamlTranslationCollector {
    public boolean collect(@NotNull String keyName, YAMLKeyValue yamlKeyValue);
}
