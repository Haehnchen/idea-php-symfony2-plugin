package fr.adrienbrault.idea.symfony2plugin.stubs;

import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * Precompiles service resource and exclude globs relative to a definition file.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceResourceGlobMatcher {
    private final List<PathMatcher> resourceMatchers;
    private final List<PathMatcher> excludeMatchers;

    private ServiceResourceGlobMatcher(@NotNull List<PathMatcher> resourceMatchers, @NotNull List<PathMatcher> excludeMatchers) {
        this.resourceMatchers = resourceMatchers;
        this.excludeMatchers = excludeMatchers;
    }

    @NotNull
    public static ServiceResourceGlobMatcher create(
        @NotNull VirtualFile serviceFileAsBase,
        @NotNull Collection<String> resources,
        @NotNull Collection<String> excludes
    ) {
        return new ServiceResourceGlobMatcher(
            compileMatchers(serviceFileAsBase, resources),
            compileMatchers(serviceFileAsBase, excludes)
        );
    }

    public boolean matches(@NotNull String phpClassPath) {
        if (this.resourceMatchers.isEmpty()) {
            return false;
        }

        if (this.resourceMatchers.stream().noneMatch(pathMatcher -> pathMatcher.matches(Paths.get(phpClassPath)))) {
            return false;
        }

        return this.excludeMatchers.stream().noneMatch(pathMatcher -> pathMatcher.matches(Paths.get(phpClassPath)));
    }

    @NotNull
    private static List<PathMatcher> compileMatchers(@NotNull VirtualFile serviceFileAsBase, @NotNull Collection<String> patterns) {
        List<PathMatcher> matchers = new ArrayList<>();

        for (String pattern : patterns.stream().filter(StringUtils::isNoneBlank).toList()) {
            PathMatcher pathMatcher = compileMatcher(serviceFileAsBase, pattern);
            if (pathMatcher != null) {
                matchers.add(pathMatcher);
            }
        }

        return matchers;
    }

    @Nullable
    private static PathMatcher compileMatcher(@NotNull VirtualFile serviceFileAsBase, @NotNull String pattern) {
        String normalizedPath = pattern.replace("\\\\", "/");

        VirtualFile serviceFile = serviceFileAsBase.getParent();
        String[] split = normalizedPath.split("/");
        String[] replacePathParts = split;
        for (String part : split) {
            if ("..".equals(part)) {
                replacePathParts = Arrays.copyOfRange(replacePathParts, 1, replacePathParts.length);
                serviceFile = serviceFile != null ? serviceFile.getParent() : null;
            } else {
                break;
            }
        }

        if (serviceFile == null) {
            return null;
        }

        String path = (serviceFile.getPath() + "/" + StringUtils.join(replacePathParts, "/"))
            .replaceAll("[^*]([*])$", "**");

        if (!path.endsWith("*")) {
            path += "**";
        }

        try {
            return FileSystems.getDefault().getPathMatcher("glob:" + path);
        } catch (PatternSyntaxException | InvalidPathException e) {
            return null;
        }
    }
}
