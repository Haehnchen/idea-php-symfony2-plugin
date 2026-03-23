package fr.adrienbrault.idea.symfony2plugin.profiler.collector;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class HttpDefaultDataCollector implements DefaultDataCollectorInterface {

    @NotNull
    private final Map<String, String> requestAttributes;

    @NotNull
    private final Collection<String> renderedTemplates;

    public HttpDefaultDataCollector(@NotNull Map<String, String> requestAttributes) {
        this(requestAttributes, Collections.emptyList());
    }

    public HttpDefaultDataCollector(@NotNull Map<String, String> requestAttributes, @NotNull List<String> renderedTemplates) {
        this.requestAttributes = requestAttributes;
        this.renderedTemplates = renderedTemplates;
    }

    @Nullable
    @Override
    public String getController() {
        return requestAttributes.get("_controller");
    }

    @Nullable
    @Override
    public String getRoute() {
        return requestAttributes.get("_route");
    }

    @Nullable
    @Override
    public String getTemplate() {
        return requestAttributes.get("_template");
    }

    @Override
    public @NotNull Collection<String> getRenderedTemplates() {
        return renderedTemplates;
    }

    @Override
    public @NotNull Collection<String> getFormTypes() {
        return Collections.emptyList();
    }
}
