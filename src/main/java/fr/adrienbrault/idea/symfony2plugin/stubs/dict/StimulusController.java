package fr.adrienbrault.idea.symfony2plugin.stubs.dict;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a Stimulus controller indexed from JavaScript files.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record StimulusController(@NotNull String name) implements Serializable {
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.name)
            .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof StimulusController
            && Objects.equals(((StimulusController) obj).name(), this.name);
    }
}
