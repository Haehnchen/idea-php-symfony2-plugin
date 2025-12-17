package fr.adrienbrault.idea.symfony2plugin.profiler.dict;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class LocalProfilerRequest implements ProfilerRequestInterface {

    @NotNull
    private final List<String> separatedLine = new ArrayList<>();

    @NotNull
    private Object[] collectors = new Object[] {};

    public LocalProfilerRequest(@NotNull String[] separatedLine) {
        Collections.addAll(this.separatedLine, separatedLine);
    }

    public LocalProfilerRequest(@NotNull String[] separatedLine, @NotNull Object... collectors) {
        this(separatedLine);
        this.collectors = collectors;
    }

    @NotNull
    public String getHash() {
        return this.separatedLine.get(0);
    }

    @Nullable
    public String getMethod() {
        return this.separatedLine.get(2);
    }

    @NotNull
    public String getUrl() {
        return this.separatedLine.get(3);
    }

    @NotNull
    @Override
    public String getProfilerUrl() {
        return "_profiler/" + this.getHash();
    }

    @Override
    public int getStatusCode() {
        if(this.separatedLine.size() <= 6) {
            return 0;
        }

        try {
            return Integer.parseInt(this.separatedLine.get(6));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getCollector(Class<T> classFactory) {
        for (Object collector : collectors) {
            if(classFactory.isAssignableFrom(collector.getClass())) {
                return (T) collector;
            }
        }

        return null;
    }
}
