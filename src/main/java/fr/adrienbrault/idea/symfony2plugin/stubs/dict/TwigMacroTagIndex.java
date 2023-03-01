package fr.adrienbrault.idea.symfony2plugin.stubs.dict;

import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigMacroTagInterface;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record TwigMacroTagIndex(@NotNull String name, @Nullable String parameters) implements TwigMacroTagInterface, Serializable {
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.name)
            .append(this.parameters)
            .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TwigMacroTagInterface &&
            Objects.equals(((TwigMacroTagInterface) obj).name(), this.name) &&
            Objects.equals(((TwigMacroTagInterface) obj).parameters(), this.parameters);
    }
}
