package fr.adrienbrault.idea.symfony2plugin.profiler.collector;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class HttpDefaultDataCollector implements DefaultDataCollectorInterface {

    @NotNull
    private Map<String, String> requestAttributes;

    public HttpDefaultDataCollector(@NotNull Map<String, String> requestAttributes) {
        this.requestAttributes = requestAttributes;
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
}
