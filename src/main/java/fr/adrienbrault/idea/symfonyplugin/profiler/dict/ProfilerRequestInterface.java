package fr.adrienbrault.idea.symfonyplugin.profiler.dict;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ProfilerRequestInterface {
    @NotNull
    String getHash();

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
