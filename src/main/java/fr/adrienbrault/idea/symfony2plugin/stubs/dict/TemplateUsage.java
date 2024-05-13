package fr.adrienbrault.idea.symfony2plugin.stubs.dict;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
  */
public class TemplateUsage implements Serializable {
    @NotNull
    private final String template;

    @NotNull
    private Collection<String> scopes;

    public TemplateUsage(@NotNull String template, @NotNull Collection<String> scopes) {
        this.template = template;
        this.scopes = scopes;
    }

    @NotNull
    public String getTemplate() {
        return template;
    }

    @NotNull
    public Collection<String> getScopes() {
        return scopes;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.template)
            .append(new HashSet<>(this.scopes))
            .toHashCode()
        ;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TemplateUsage &&
            Objects.equals(((TemplateUsage) obj).getTemplate(), this.template) &&
            Objects.equals(new HashSet<>(((TemplateUsage) obj).getScopes()), new HashSet<>(this.scopes))
        ;
    }
}
