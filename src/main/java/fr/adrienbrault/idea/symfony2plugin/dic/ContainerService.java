package fr.adrienbrault.idea.symfony2plugin.dic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ContainerService {
    private static final Comparator<ContainerServiceMetadata> METADATA_PRIORITY = Comparator.comparingInt(
        metadata -> switch (metadata.sourceKind()) {
            case INDEXED_SERVICE -> 0;
            case COMPILED_CONTAINER -> 1;
            case RESOURCE_PROTOTYPE -> 2;
        }
    );

    final private String name;

    final private String className;
    private final Set<String> classVariants = new HashSet<>();
    private final Set<ContainerServiceMetadata> metadata = new LinkedHashSet<>();
    @Nullable
    private Set<String> cachedClassNames;
    @Nullable
    private Boolean cachedMetadataAutowire;
    @Nullable
    private Boolean cachedMetadataAutoconfigure;
    @Nullable
    private Boolean cachedResourcePrototypeMetadata;
    @Nullable
    private Set<String> cachedResourceServiceIds;
    @Nullable
    private Set<String> cachedTags;
    @Nullable
    private Boolean cachedDeprecated;
    @Nullable
    private Boolean cachedPrivate;
    @Nullable
    private Boolean cachedWeak;
    @Nullable
    private Set<String> cachedParents;
    @Nullable
    private Set<String> cachedDecorates;
    @Nullable
    private Set<String> cachedDecorationInnerNames;

    public ContainerService(@NotNull String name, @Nullable String className, @NotNull ContainerServiceMetadata metadata) {
        this.name = name;
        this.className = className;
        addMetadata(metadata);
    }

    /**
     *
     * @return can be null, class, or a parameter name
     */
    @Nullable
    public String getClassName() {
        return className;
    }

    public void addClassName(@NotNull String className) {
        this.classVariants.add(className);
        this.cachedClassNames = null;
    }

    public void addMetadata(@NotNull ContainerServiceMetadata metadata) {
        if (this.metadata.add(metadata)) {
            this.cachedMetadataAutowire = null;
            this.cachedMetadataAutoconfigure = null;
            this.cachedResourcePrototypeMetadata = null;
            this.cachedResourceServiceIds = null;
            this.cachedTags = null;
            this.cachedDeprecated = null;
            this.cachedPrivate = null;
            this.cachedWeak = null;
            this.cachedParents = null;
            this.cachedDecorates = null;
            this.cachedDecorationInnerNames = null;
        }
    }

    @NotNull
    public Set<String> getClassNames() {
        if (cachedClassNames != null) {
            return cachedClassNames;
        }

        Set<String> variants = new HashSet<>();

        if(className != null) {
            variants.add(className);
        }

        variants.addAll(classVariants);

        return cachedClassNames = Collections.unmodifiableSet(variants);
    }

    public boolean isWeak() {
        if (cachedWeak != null) {
            return cachedWeak;
        }

        if (!metadata.isEmpty()) {
            return cachedWeak = !hasSourceKind(ContainerServiceMetadata.SourceKind.COMPILED_CONTAINER);
        }

        return cachedWeak = false;
    }

    public boolean isPrivate() {
        if (cachedPrivate != null) {
            return cachedPrivate;
        }

        ContainerServiceMetadata metadata = getPrimaryMetadata();
        return cachedPrivate = metadata != null && !metadata.publicService();
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public List<ContainerServiceMetadata> getMetadata() {
        return metadata.stream().sorted(METADATA_PRIORITY).toList();
    }

    public boolean isAutowireEnabled() {
        if (cachedMetadataAutowire != null) {
            return cachedMetadataAutowire;
        }

        for (ContainerServiceMetadata containerServiceMetadata : metadata) {
            if (containerServiceMetadata.autowire()) {
                return cachedMetadataAutowire = true;
            }
        }

        return cachedMetadataAutowire = false;
    }

    public boolean isAutoconfigureEnabled() {
        if (cachedMetadataAutoconfigure != null) {
            return cachedMetadataAutoconfigure;
        }

        for (ContainerServiceMetadata containerServiceMetadata : metadata) {
            if (containerServiceMetadata.autoconfigure()) {
                return cachedMetadataAutoconfigure = true;
            }
        }

        return cachedMetadataAutoconfigure = false;
    }

    public boolean hasResourcePrototypeMetadata() {
        if (cachedResourcePrototypeMetadata != null) {
            return cachedResourcePrototypeMetadata;
        }

        for (ContainerServiceMetadata containerServiceMetadata : metadata) {
            if (containerServiceMetadata.sourceKind() == ContainerServiceMetadata.SourceKind.RESOURCE_PROTOTYPE) {
                return cachedResourcePrototypeMetadata = true;
            }
        }

        return cachedResourcePrototypeMetadata = false;
    }

    @NotNull
    public Set<String> getResourceServiceIds() {
        if (cachedResourceServiceIds != null) {
            return cachedResourceServiceIds;
        }

        Set<String> resourceServiceIds = new LinkedHashSet<>();
        for (ContainerServiceMetadata containerServiceMetadata : metadata) {
            String resourceServiceId = containerServiceMetadata.resourceServiceId();
            if (resourceServiceId != null) {
                resourceServiceIds.add(resourceServiceId);
            }
        }

        return cachedResourceServiceIds = Collections.unmodifiableSet(resourceServiceIds);
    }

    @NotNull
    public Set<String> getTags() {
        if (cachedTags != null) {
            return cachedTags;
        }

        Set<String> tags = new LinkedHashSet<>();
        for (ContainerServiceMetadata containerServiceMetadata : metadata) {
            tags.addAll(containerServiceMetadata.tags());
        }

        return cachedTags = Collections.unmodifiableSet(tags);
    }

    @NotNull
    public Set<String> getParents() {
        if (cachedParents != null) {
            return cachedParents;
        }

        return cachedParents = collectScalarValues(ContainerServiceMetadata::parent);
    }

    @NotNull
    public Set<String> getDecoratesValues() {
        if (cachedDecorates != null) {
            return cachedDecorates;
        }

        return cachedDecorates = collectScalarValues(ContainerServiceMetadata::decorates);
    }

    @NotNull
    public Set<String> getDecorationInnerNames() {
        if (cachedDecorationInnerNames != null) {
            return cachedDecorationInnerNames;
        }

        return cachedDecorationInnerNames = collectScalarValues(ContainerServiceMetadata::decorationInnerName);
    }

    public boolean isDeprecated() {
        if (cachedDeprecated != null) {
            return cachedDeprecated;
        }

        ContainerServiceMetadata metadata = getPrimaryMetadata();
        return cachedDeprecated = metadata != null && metadata.deprecated();
    }

    public boolean hasSourceKind(@NotNull ContainerServiceMetadata.SourceKind sourceKind) {
        for (ContainerServiceMetadata containerServiceMetadata : metadata) {
            if (containerServiceMetadata.sourceKind() == sourceKind) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    private ContainerServiceMetadata getPrimaryMetadata() {
        return metadata.stream().min(METADATA_PRIORITY).orElse(null);
    }

    @NotNull
    private Set<String> collectScalarValues(@NotNull Function<ContainerServiceMetadata, String> extractor) {
        Set<String> values = new LinkedHashSet<>();
        for (ContainerServiceMetadata containerServiceMetadata : getMetadata()) {
            String value = extractor.apply(containerServiceMetadata);
            if (value != null && !value.isBlank()) {
                values.add(value);
            }
        }

        return Collections.unmodifiableSet(values);
    }
}
