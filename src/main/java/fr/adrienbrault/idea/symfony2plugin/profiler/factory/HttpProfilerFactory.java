package fr.adrienbrault.idea.symfony2plugin.profiler.factory;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.profiler.HttpProfilerIndex;
import fr.adrienbrault.idea.symfony2plugin.profiler.ProfilerIndexInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.utils.ProfilerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class HttpProfilerFactory implements ProfilerFactoryInterface {
    @Nullable
    @Override
    public ProfilerIndexInterface createProfilerIndex(@NotNull Project project) {
        String profilerHttpUrl = ProfilerUtil.normalizeHttpProfilerBaseUrl(Settings.getInstance(project).profilerHttpUrl);
        if(profilerHttpUrl == null) {
            return null;
        }

        return new HttpProfilerIndex(project, profilerHttpUrl);
    }

    @Override
    public boolean accepts(@NotNull Project project) {
        return Settings.getInstance(project).profilerHttpEnabled;
    }
}
