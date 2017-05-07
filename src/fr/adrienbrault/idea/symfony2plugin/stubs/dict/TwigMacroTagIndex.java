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
public class TwigMacroTagIndex implements TwigMacroTagInterface, Serializable {

    @NotNull
    private final String name;

    @Nullable
    private final String parameters;

    public TwigMacroTagIndex(@NotNull String name, @Nullable String parameters) {
        this.name = name;
        this.parameters = parameters;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    public String getParameters() {
        return parameters;
    }

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
            Objects.equals(((TwigMacroTagInterface) obj).getName(), this.name) &&
            Objects.equals(((TwigMacroTagInterface) obj).getParameters(), this.parameters);
    }
}
