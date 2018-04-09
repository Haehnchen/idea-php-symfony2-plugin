package fr.adrienbrault.idea.symfony2plugin.profiler.factory;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.profiler.ProfilerIndexInterface;
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
