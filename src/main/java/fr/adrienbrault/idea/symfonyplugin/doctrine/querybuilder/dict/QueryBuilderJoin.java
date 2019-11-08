package fr.adrienbrault.idea.symfonyplugin.doctrine.querybuilder.dict;

import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class QueryBuilderJoin {

    final private String join;
    final private String alias;
    private String resolvedClass;

    public QueryBuilderJoin(String join, String alias) {
        this.join = join;
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }

    public String getJoin() {
        return join;
    }

    @Nullable
    public String getResolvedClass() {
        return resolvedClass;
    }

    public void setResolvedClass(String resolvedClass) {
        this.resolvedClass = resolvedClass;
    }

}
