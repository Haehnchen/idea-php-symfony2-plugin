package fr.adrienbrault.idea.symfony2plugin.dic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Carries service metadata for one contributing definition so ContainerService can keep
 * multiple sources side by side; any merged view is computed by the consumer later.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record ContainerServiceMetadata(
    // Optional resource/prototype definition id that expanded this class, eg "App\\"
    @Nullable String resourceServiceId,
    boolean autowire,
    boolean autoconfigure,
    @NotNull Set<String> tags,
    // Raw resource glob patterns from the contributing resource/prototype definition
    @NotNull Set<String> resource,
    // Raw exclude patterns from the contributing resource/prototype definition
    @NotNull Set<String> exclude,
    @NotNull SourceKind sourceKind
) {
    public ContainerServiceMetadata(
        @Nullable String resourceServiceId,
        boolean autowire,
        boolean autoconfigure,
        @NotNull Collection<String> tags,
        @NotNull Collection<String> resource,
        @NotNull Collection<String> exclude,
        @NotNull SourceKind sourceKind
    ) {
        this(
            resourceServiceId,
            autowire,
            autoconfigure,
            normalize(tags),
            normalize(resource),
            normalize(exclude),
            sourceKind
        );
    }

    private static Set<String> normalize(@NotNull Collection<String> values) {
        return values.isEmpty()
            ? Collections.emptySet()
            : Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    public enum SourceKind {
        RESOURCE_PROTOTYPE
    }
}
