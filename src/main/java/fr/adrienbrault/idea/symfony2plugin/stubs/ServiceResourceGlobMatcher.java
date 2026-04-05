package fr.adrienbrault.idea.symfony2plugin.stubs;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Precompiles service resource and exclude globs relative to a definition file.
 * Mirrors Symfony service auto-discovery semantics used by DependencyInjection FileLoader::registerClasses().
 * Symfony does not treat these values as plain path regexes only: it first resolves the relative resource
 * against the definition file, then scans from that resolved prefix recursively through Config GlobResource.
 * Because of that, directory-like inputs such as "../src/", "../src/Controller" or "../src/*" behave as
 * recursive service-discovery roots, and excludes follow the same recursive matching model.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceResourceGlobMatcher {
    private final List<Pattern> resourceMatchers;
    private final List<Pattern> excludeMatchers;

    private ServiceResourceGlobMatcher(@NotNull List<Pattern> resourceMatchers, @NotNull List<Pattern> excludeMatchers) {
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

    /**
     * Matches a candidate class file against the compiled resource patterns and excludes.
     */
    public boolean matches(@NotNull VirtualFile phpClassFile) {
        if (this.resourceMatchers.isEmpty()) {
            return false;
        }

        // Match against normalized VFS paths only; no IO-file conversion is needed here.
        String path = normalizePath(phpClassFile.getPath());

        if (this.resourceMatchers.stream().noneMatch(pattern -> pattern.matcher(path).matches())) {
            return false;
        }

        return this.excludeMatchers.stream().noneMatch(pattern -> pattern.matcher(path).matches());
    }

    /**
     * Compiles all non-blank resource or exclude patterns relative to the service file.
     */
    @NotNull
    private static List<Pattern> compileMatchers(@NotNull VirtualFile serviceFileAsBase, @NotNull Collection<String> patterns) {
        List<Pattern> matchers = new ArrayList<>();

        for (String pattern : patterns.stream().filter(StringUtils::isNoneBlank).toList()) {
            Pattern compiled = compileMatcher(serviceFileAsBase, pattern);
            if (compiled != null) {
                matchers.add(compiled);
            }
        }

        return matchers;
    }

    /**
     * Compiles a single expanded service resource pattern to a regex matcher.
     * Uses IntelliJ Ant conversion for simple patterns and falls back to the Symfony-aware converter otherwise.
     */
    @Nullable
    private static Pattern compileMatcher(@NotNull VirtualFile serviceFileAsBase, @NotNull String pattern) {
        String matcherPatternPath = resolveMatcherPatternPath(serviceFileAsBase, pattern);
        if (matcherPatternPath == null) {
            return null;
        }

        try {
            // Let the platform handle simple Ant-compatible patterns directly.
            if (isAntSafePattern(matcherPatternPath)) {
                return Pattern.compile("^" + FileUtil.convertAntToRegexp(matcherPatternPath, false) + "$");
            }

            // Symfony resource patterns still need the custom string-based conversion.
            StringBuilder regex = new StringBuilder("^");
            if (!appendGlobRegex(matcherPatternPath, regex)) {
                return null;
            }

            regex.append("$");
            return Pattern.compile(regex.toString());
        } catch (PatternSyntaxException ignored) {
            return null;
        }
    }

    /**
     * Expands a resource pattern relative to the service file and preserves the previous recursive-directory semantics.
     */
    @Nullable
    private static String resolveMatcherPatternPath(@NotNull VirtualFile serviceFileAsBase, @NotNull String pattern) {
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

        // Preserve the old expansion semantics so directory resources stay recursive.
        String serviceFilePath = normalizePath(serviceFile.getPath());

        String path = (serviceFilePath + "/" + StringUtils.join(replacePathParts, "/"))
            .replaceAll("[^*]([*])$", "**");

        if (!path.endsWith("*")) {
            path += "**";
        }

        return path;
    }

    /**
     * Converts the supported Symfony glob subset to a regex body.
     */
    private static boolean appendGlobRegex(@NotNull String pattern, @NotNull StringBuilder regex) {
        // Supports the Symfony patterns used here: *, **, ?, and simple {a,b} groups.
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);

            if (c == '*') {
                boolean doubleStar = i + 1 < pattern.length() && pattern.charAt(i + 1) == '*';
                regex.append(doubleStar ? ".*" : "[^/]*");
                if (doubleStar) {
                    i++;
                }
                continue;
            }

            if (c == '?') {
                regex.append("[^/]");
                continue;
            }

            if (c == '{') {
                int end = findClosingBrace(pattern, i);
                if (end == -1) {
                    return false;
                }

                String inner = pattern.substring(i + 1, end);
                if (inner.contains("{") || inner.contains("}")) {
                    return false;
                }

                String[] variants = inner.split(",", -1);
                regex.append("(?:");
                for (int variantIndex = 0; variantIndex < variants.length; variantIndex++) {
                    if (variantIndex > 0) {
                        regex.append("|");
                    }

                    if (!appendGlobRegex(variants[variantIndex], regex)) {
                        return false;
                    }
                }
                regex.append(")");
                i = end;
                continue;
            }

            appendEscapedChar(regex, c);
        }

        return true;
    }

    /**
     * Detects patterns that can be delegated to IntelliJ's Ant-pattern conversion without changing semantics.
     * Brace groups and recursive wildcards stay on the custom conversion path.
     */
    private static boolean isAntSafePattern(@NotNull String pattern) {
        return !pattern.contains("{") && !pattern.contains("}") && !pattern.contains("**");
    }

    /**
     * Finds the closing brace for a simple top-level "{a,b}" group.
     */
    private static int findClosingBrace(@NotNull String pattern, int start) {
        for (int i = start + 1; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '{') {
                return -1;
            }

            if (c == '}') {
                return i;
            }
        }

        return -1;
    }

    /**
     * Escapes regex metacharacters while keeping path separators normalized to '/'.
     */
    private static void appendEscapedChar(@NotNull StringBuilder regex, char c) {
        if ("\\.[]{}()+-^$|".indexOf(c) >= 0) {
            regex.append("\\");
        }

        regex.append(c == '\\' ? '/' : c);
    }

    /**
     * Normalizes path separators to the VFS-style '/' form used by the matcher.
     */
    @NotNull
    private static String normalizePath(@NotNull String path) {
        return path.replace('\\', '/');
    }
}
