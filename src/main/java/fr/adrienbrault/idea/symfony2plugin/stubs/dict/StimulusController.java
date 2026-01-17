package fr.adrienbrault.idea.symfony2plugin.stubs.dict;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a Stimulus controller indexed from JavaScript files or controllers.json.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record StimulusController(@NotNull String name, @NotNull SourceType sourceType, @Nullable String originalName) implements Serializable {
    public enum SourceType {
        /** Controller from JavaScript/TypeScript file (e.g., hello_controller.js) */
        JAVASCRIPT,
        /** Controller from controllers.json (e.g., "@symfony/ux-chartjs": { "chart": {...} }) */
        CONTROLLERS_JSON
    }

    /**
     * Constructor for JavaScript file controllers (no original name, normalized name only)
     */
    public StimulusController(@NotNull String name) {
        this(name, SourceType.JAVASCRIPT, null);
    }

    /**
     * Constructor for controllers.json with original name
     */
    public StimulusController(@NotNull String normalizedName, @NotNull String originalName) {
        this(normalizedName, SourceType.CONTROLLERS_JSON, originalName);
    }

    /**
     * Get the name for HTML data-controller attribute (always normalized)
     * For JS files: "hello", "users--list"
     * For JSON: "symfony--ux-chartjs--chart"
     */
    @NotNull
    public String getNormalizedName() {
        return name;
    }

    /**
     * Get the original name for Twig stimulus_controller() function
     * For JS files: same as normalized (e.g., "hello", "users--list")
     * For JSON: "@symfony/ux-chartjs/chart"
     */
    @NotNull
    public String getTwigName() {
        if (sourceType == SourceType.CONTROLLERS_JSON && originalName != null) {
            return originalName;
        }
        return name;
    }

    /**
     * Check if this controller has an original name (from controllers.json)
     */
    public boolean hasOriginalName() {
        return sourceType == SourceType.CONTROLLERS_JSON && originalName != null;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.name)
            .append(this.sourceType)
            .append(this.originalName)
            .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof StimulusController(String name1, SourceType type, String originalName1))) return false;
        return Objects.equals(this.name, name1)
            && this.sourceType == type
            && Objects.equals(this.originalName, originalName1);
    }
}
