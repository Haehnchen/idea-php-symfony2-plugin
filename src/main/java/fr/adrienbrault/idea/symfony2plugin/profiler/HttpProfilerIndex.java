package fr.adrienbrault.idea.symfony2plugin.profiler;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequestInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.utils.ProfilerUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class HttpProfilerIndex implements ProfilerIndexInterface {
    /**
     * http://127.0.0.1:8080/_profiler
     */
    private static String PROFILER_PATH = "_profiler";

    @NotNull
    private final Project project;

    @NotNull
    private final String url;

    public HttpProfilerIndex(@NotNull Project project, @NotNull String url) {
        this.project = project;
        this.url = StringUtils.stripEnd(url, "/");
    }

    @NotNull
    @Override
    public List<ProfilerRequestInterface> getRequests() {
        String content = ProfilerUtil.getProfilerUrlContent(String.format("%s/%s/empty/search/results?ip=&limit=10", this.url, PROFILER_PATH));
        if(content == null) {
            return Collections.emptyList();
        }

        return new ArrayList<>(ProfilerUtil.collectHttpDataForRequest(
            project, ProfilerUtil.createRequestsFromIndexHtml(this.project, content, this.url))
        );
    }

    @Nullable
    @Override
    public String getUrlForRequest(@NotNull ProfilerRequestInterface request) {
        String profilerUrl = request.getProfilerUrl();

        // already absolute url given
        if(profilerUrl.startsWith("http://") || profilerUrl.startsWith("https://")) {
            return profilerUrl;
        }

        return this.url  + "/" + StringUtils.stripStart(profilerUrl, "/");
    }
}
