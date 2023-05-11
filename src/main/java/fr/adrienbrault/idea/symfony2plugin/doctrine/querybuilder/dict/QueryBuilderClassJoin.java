package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record QueryBuilderClassJoin(String className, String alias) {
    public QueryBuilderClassJoin(@NotNull String className, @NotNull String alias) {
        this.className = className;
        this.alias = alias;
    }
}
