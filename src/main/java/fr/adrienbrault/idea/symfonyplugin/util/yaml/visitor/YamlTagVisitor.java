package fr.adrienbrault.idea.symfonyplugin.util.yaml.visitor;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface YamlTagVisitor {
    void visit(@NotNull YamlServiceTag args);
}
