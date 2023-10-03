package fr.adrienbrault.idea.symfony2plugin.profiler;

import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.profiler.collector.LocalDefaultDataCollector;
import fr.adrienbrault.idea.symfony2plugin.profiler.collector.LocalMailCollector;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.LocalProfilerRequest;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequestInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.reader.ReverseFileLineReader;
import fr.adrienbrault.idea.symfony2plugin.profiler.utils.ProfilerUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class LocalProfilerIndex implements ProfilerIndexInterface {
    @NotNull
    private File file;

    @Nullable
    private String baseUrl;

    public LocalProfilerIndex(@NotNull File file) {
        this.file = file;
    }

    public LocalProfilerIndex(@NotNull File file, @Nullable String baseUrl) {
        this.file = file;
        this.baseUrl = baseUrl;
    }

    @NotNull
    public List<ProfilerRequestInterface> getRequests() {
        List<String> lines = new ArrayList<>();

        try {
            // empty line and end of line need +1
            ContainerUtil.addAll(lines, new ReverseFileLineReader(this.file, "UTF-8", 11).readLines());
        } catch (IOException ignored) {
        }

        Collection<Callable<ProfilerRequestInterface>> callable = new ArrayList<>();

        // build thread callable collection
        lines.stream().filter(StringUtils::isNotBlank).forEachOrdered(line -> {

            // we need at least this information for a valid line:
            // "18e6b8,127.0.0.1,GET,http://127.0.0.1:8000/foobar"
            String[] split = line.split(",");
            if (split.length <= 4) {
                return;
            }

            callable.add(new MyProfilerRequestBuilderCallable(split));
        });

        return ProfilerUtil.getProfilerRequestCollectorDecorated(callable, 15);
    }

    @Nullable
    @Override
    public String getUrlForRequest(@NotNull ProfilerRequestInterface request) {
        if(this.baseUrl != null) {
            return this.baseUrl  + "/" + StringUtils.stripStart(request.getProfilerUrl(), "/");
        }

        return ProfilerUtil.getBaseProfilerUrlFromRequest(request) + "/" + StringUtils.stripStart(request.getProfilerUrl(), "/");
    }

    @NotNull
    private String getPath(@NotNull String hash) {
        String[] hashSplit = hash.split("(?<=\\G.{2})");
        return hashSplit[2] + "/" + hashSplit[1] + "/" + hash;
    }

    @Nullable
    private File getFile(@NotNull String hash) {
        String path = this.getPath(hash);

        File file = new File(this.file.getParentFile().getAbsolutePath() + "/" + path);
        if(!file.exists()) {
            return null;
        }

        return file;
    }

    @Nullable
    private String getContentForHash(@NotNull String hash) {
        File file = this.getFile(hash);
        if(file == null) {
            return  null;
        }

        return ProfilerUtil.getContentForFile(file);
    }

    private class MyProfilerRequestBuilderCallable implements Callable<ProfilerRequestInterface> {
        private final String[] split;

        MyProfilerRequestBuilderCallable(String[] split) {
            this.split = split;
        }

        @Override
        public ProfilerRequestInterface call() {
            String content = getContentForHash(split[0]);
            if(content == null) {
                return new LocalProfilerRequest(split);
            }

            return new LocalProfilerRequest(
                split,
                new LocalDefaultDataCollector(content),
                new LocalMailCollector(content)
            );
        }
    }
}
