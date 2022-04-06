package fr.adrienbrault.idea.symfony2plugin.stubs.dict;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

import static fr.adrienbrault.idea.symfony2plugin.templating.dict.TemplateInclude.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateInclude implements Serializable {

    @NotNull
    private final String template;
    private final TYPE type;

    public TemplateInclude(@NotNull String template, @NotNull TYPE type) {
        this.template = template;
        this.type = type;
    }

    @Nullable
    public TYPE getType() {
        return type;
    }

    @NotNull
    public String getTemplate() {
        return template;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.type)
            .append(this.template)
            .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TemplateInclude
            && Objects.equals(((TemplateInclude) obj).template, this.template)
            && Objects.equals(((TemplateInclude) obj).type, this.type);
    }
}
