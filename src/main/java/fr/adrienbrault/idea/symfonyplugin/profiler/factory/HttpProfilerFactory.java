package fr.adrienbrault.idea.symfonyplugin.profiler.factory;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfonyplugin.Settings;
import fr.adrienbrault.idea.symfonyplugin.profiler.HttpProfilerIndex;
import fr.adrienbrault.idea.symfonyplugin.profiler.ProfilerIndexInterface;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class HttpProfilerFactory implements ProfilerFactoryInterface {
    @Nullable
    @Override
    public ProfilerIndexInterface createProfilerIndex(@NotNull Project project) {
        String profilerHttpUrl = Settings.getInstance(project).profilerHttpUrl;
        if(!profilerHttpUrl.startsWith("http")) {
            return null;
        }

        return new HttpProfilerIndex(project, StringUtils.stripEnd(profilerHttpUrl, "/"));
    }

    @Override
    public boolean accepts(@NotNull Project project) {
        return Settings.getInstance(project).profilerHttpEnabled;
    }
}
