package fr.adrienbrault.idea.symfony2plugin.profiler.dict;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class HttpProfilerRequest implements ProfilerRequestInterface {
    private final int statusCode;

    @NotNull
    private final String hash;

    @NotNull
    private final String profilerUrl;

    @Nullable
    private final String method;

    @NotNull
    private final String url;

    @NotNull
    private final Object[] collectors;

    public HttpProfilerRequest(ProfilerRequestInterface request, @NotNull Object... collectors) {
        this(request.getStatusCode(), request.getHash(), request.getProfilerUrl(), request.getMethod(), request.getUrl(), collectors);
    }

    public HttpProfilerRequest(int statusCode, @NotNull String hash, @NotNull String profilerUrl, @Nullable String method, @NotNull String url, @Nullable Object... collectors) {
        this.statusCode = statusCode;
        this.hash = hash;
        this.profilerUrl = profilerUrl;
        this.method = method;
        this.url = url;
        this.collectors = collectors != null ? collectors : new Object[] {};
    }

    @NotNull
    @Override
    public String getHash() {
        return hash;
    }

    @Nullable
    @Override
    public String getMethod() {
        return method;
    }

    @NotNull
    @Override
    public String getUrl() {
        return url;
    }

    @NotNull
    @Override
    public String getProfilerUrl() {
        return this.profilerUrl;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Nullable
    @Override
    public <T> T getCollector(Class<T> classFactory) {
        for (Object collector : collectors) {
            if(classFactory.isAssignableFrom(collector.getClass())) {
                return (T) collector;
            }
        }

        return null;
    }
}
