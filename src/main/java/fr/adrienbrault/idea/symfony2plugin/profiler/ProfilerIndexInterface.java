package fr.adrienbrault.idea.symfony2plugin.profiler;

import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequestInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ProfilerIndexInterface {
    @NotNull
    List<ProfilerRequestInterface> getRequests();

    @Nullable
    String getUrlForRequest(@NotNull ProfilerRequestInterface request);
}
