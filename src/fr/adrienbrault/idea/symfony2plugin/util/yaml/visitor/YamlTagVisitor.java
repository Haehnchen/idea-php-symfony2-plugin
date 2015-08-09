package fr.adrienbrault.idea.symfony2plugin.util.yaml.visitor;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface YamlTagVisitor {
    void visit(@NotNull YamlTagVisitorArguments args);
}
