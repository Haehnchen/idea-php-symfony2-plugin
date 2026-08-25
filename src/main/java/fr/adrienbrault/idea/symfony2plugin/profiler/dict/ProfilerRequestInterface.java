package fr.adrienbrault.idea.symfony2plugin.profiler.dict;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ProfilerRequestInterface {
    @NotNull
    String getHash();

    /** Unix timestamp in seconds, when exposed by the profiler index. */
    @Nullable
    default Long getTime() {
        return null;
    }

    @Nullable
    String getMethod();

    @NotNull
    String getUrl();

    @NotNull
    String getProfilerUrl();

    int getStatusCode();

    @Nullable
    <T> T getCollector(Class<T> classFactory);
}
