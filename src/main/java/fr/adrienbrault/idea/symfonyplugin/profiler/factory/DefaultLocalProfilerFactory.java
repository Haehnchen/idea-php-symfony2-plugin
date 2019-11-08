package fr.adrienbrault.idea.symfonyplugin.profiler.factory;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfonyplugin.profiler.LocalProfilerIndex;
import fr.adrienbrault.idea.symfonyplugin.profiler.ProfilerIndexInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class DefaultLocalProfilerFactory extends LocalProfilerFactory {
    @Nullable
    @Override
    public ProfilerIndexInterface createProfilerIndex(@NotNull Project project) {
        File csvProfilerFile = getCsvIndex(project);
        if(csvProfilerFile == null) {
            return null;
        }

        return new LocalProfilerIndex(csvProfilerFile, null);
    }

    @Override
    public boolean accepts(@NotNull Project project) {
        return true;
    }
}
