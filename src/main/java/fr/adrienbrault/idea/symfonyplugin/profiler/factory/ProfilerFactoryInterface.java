package fr.adrienbrault.idea.symfonyplugin.profiler.factory;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfonyplugin.profiler.ProfilerIndexInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ProfilerFactoryInterface {
    @Nullable
    ProfilerIndexInterface createProfilerIndex(@NotNull Project project);

    boolean accepts(@NotNull Project project);
}
