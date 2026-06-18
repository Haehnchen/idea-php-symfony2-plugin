package fr.adrienbrault.idea.symfony2plugin.profiler.collector;

import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerTwigComponent;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface TwigComponentDataCollectorInterface {
    @NotNull
    Collection<ProfilerTwigComponent> getComponents();
}
