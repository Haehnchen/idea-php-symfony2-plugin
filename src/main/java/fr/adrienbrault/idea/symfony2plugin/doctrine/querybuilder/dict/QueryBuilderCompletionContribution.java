package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record QueryBuilderCompletionContribution(@NotNull QueryBuilderCompletionContributionType type, @NotNull String prefix) {
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.type)
            .append(this.prefix)
            .toHashCode();
    }
}
