package fr.adrienbrault.idea.symfonyplugin.profiler.factory;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfonyplugin.profiler.ProfilerIndexInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ProfilerFactoryUtil {
    private static ProfilerFactoryInterface[] PROFILER = new ProfilerFactoryInterface[] {
        new HttpProfilerFactory(),
        new LocalProfilerFactory(),
    };

    @Nullable
    public static ProfilerIndexInterface createIndex(@NotNull Project project) {
        for (ProfilerFactoryInterface factory : PROFILER) {
            if(!factory.accepts(project)) {
                continue;
            }

            ProfilerIndexInterface profiler = factory.createProfilerIndex(project);
            if(profiler != null) {
                return profiler;
            }
        }

        // non user setting try to use local filesystem with self searching path
        return new DefaultLocalProfilerFactory().createProfilerIndex(project);
    }
}
