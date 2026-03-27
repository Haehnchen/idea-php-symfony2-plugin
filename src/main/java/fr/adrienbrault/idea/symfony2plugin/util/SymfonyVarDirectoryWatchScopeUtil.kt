package fr.adrienbrault.idea.symfony2plugin.util

import fr.adrienbrault.idea.symfony2plugin.Settings
import com.intellij.openapi.util.io.FileUtil

/**
 * Shared path normalization and scope classification logic for [SymfonyVarDirectoryWatcher].
 *
 * Keeps VFS event matching deterministic across platforms by normalizing separators,
 * resolving `.` / `..`, and folding Windows drive letters to a stable lowercase form.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
object SymfonyVarDirectoryWatchScopeUtil {
    /**
     * Self-contained watch scope definition with normalized roots and an optional file predicate.
     */
    data class ScopeEntry(
        val scope: SymfonyVarDirectoryWatcher.Scope,
        val roots: Set<String>,
        val predicate: ((String) -> Boolean)? = null,
    )

    /**
     * Builds the watcher scopes from the current project base path and plugin settings.
     *
     * Directory-like roots are stored with a trailing slash while configured file roots
     * remain plain normalized file paths.
     */
    fun createScopes(
        base: String,
        settings: Settings,
    ): List<ScopeEntry> {
        fun resolve(path: String): String {
            val normalized = normalizePath(path)
            return if (isAbsolutePath(normalized)) normalized else normalizePath("$base/$normalized")
        }

        val containerRoots = buildSet {
            add(normalizeRootPath("$base/var/cache"))
            add(normalizeRootPath("$base/app/cache"))
            settings.containerFiles.orEmpty().mapNotNull { it.path }.filter { it.isNotBlank() }
                .forEach { add(normalizePath(resolve(it))) }
        }

        val explicitTranslationRoots = buildSet {
            settings.containerFiles.orEmpty().mapNotNull { it.path }.filter { it.isNotBlank() }
                .map(::resolve)
                .mapNotNull { normalizedContainerPath ->
                    normalizedContainerPath.substringBeforeLast('/', "")
                        .takeIf { it.isNotBlank() }
                        ?.let { normalizeRootPath("$it/translations") }
                }
                .forEach(::add)

            val configuredPath = settings.pathToTranslation
            if (!configuredPath.isNullOrBlank()) {
                add(normalizeRootPath(resolve(configuredPath)))
            }
        }

        val translationRoots = buildSet {
            add(normalizeRootPath("$base/var/cache"))
            add(normalizeRootPath("$base/app/cache"))
            addAll(explicitTranslationRoots)
        }

        val routeRoots = buildSet {
            Settings.DEFAULT_ROUTES.forEach { add(normalizePath(resolve(it))) }
            settings.routingFiles.orEmpty().mapNotNull { it.path }.filter { it.isNotBlank() }
                .forEach { add(normalizePath(resolve(it))) }
        }

        return listOf(
            ScopeEntry(
                scope = SymfonyVarDirectoryWatcher.Scope.CONTAINER,
                roots = containerRoots,
                // Container scope only reacts to compiled container XML files in dev/dev_* cache directories.
                predicate = { isCompiledContainerPath(it) },
            ),
            ScopeEntry(
                scope = SymfonyVarDirectoryWatcher.Scope.TRANSLATIONS,
                roots = translationRoots,
                predicate = { isTranslationPath(it, explicitTranslationRoots) },
            ),
            ScopeEntry(
                scope = SymfonyVarDirectoryWatcher.Scope.ROUTES,
                roots = routeRoots,
                predicate = { isCompiledRoutePath(it) },
            ),
        )
    }

    /**
     * Returns the scopes that should be invalidated for the given event paths.
     *
     * Paths are normalized first so callers can pass raw VFS event paths directly.
     */
    fun collectScopesForPaths(
        scopes: List<ScopeEntry>,
        paths: List<String>,
    ): Set<SymfonyVarDirectoryWatcher.Scope> {
        val normalizedPaths = paths.map(::normalizePath)
        val toInvalidate = mutableSetOf<SymfonyVarDirectoryWatcher.Scope>()

        for (entry in scopes) {
            if (entry.scope in toInvalidate) {
                continue
            }

            if (normalizedPaths.any { path -> entry.roots.any { root -> isRelevantPath(path, root) } }
                && (entry.predicate == null || normalizedPaths.any { entry.predicate.invoke(it) })) {
                toInvalidate += entry.scope
            }
        }

        return toInvalidate
    }

    /**
     * Returns true for translation-related paths discovered from cache conventions or explicit roots.
     */
    fun isTranslationPath(path: String, explicitRoots: Set<String> = emptySet()): Boolean {
        val normalized = normalizePath(path)

        if (explicitRoots.any { root -> isRelevantPath(normalized, root) }) {
            return true
        }

        return isCompiledTranslationCachePath(normalized)
    }

    /**
     * Returns true for cache paths that can affect compiled translations in `dev` or `dev_*`.
     */
    fun isCompiledTranslationCachePath(path: String): Boolean {
        val lowered = normalizePath(path).lowercase()

        return sequenceOf("/var/cache", "/app/cache").any { cacheRoot ->
            val index = lowered.indexOf(cacheRoot)
            if (index < 0) {
                return@any false
            }

            val remainder = lowered.substring(index + cacheRoot.length).trimStart('/')
            if (remainder.isEmpty()) {
                return@any true
            }

            val segments = remainder.split('/')
            val devSegment = segments.firstOrNull() ?: return@any false
            if (devSegment != "dev" && !devSegment.startsWith("dev_")) {
                return@any false
            }

            val afterDev = segments.drop(1)
            afterDev.isEmpty() || afterDev.firstOrNull() == "translations"
        }
    }

    /**
     * Returns true when [path] is inside a Symfony `var/cache` or `app/cache` directory.
     */
    fun isCachePath(path: String): Boolean {
        val lowered = normalizePath(path).lowercase()
        return lowered.contains("/var/cache") || lowered.contains("/app/cache")
    }

    /**
     * Returns true for compiled Symfony route PHP files.
     *
     * Matches the URL-generator files produced by `RouteHelper.getDefaultRoutes`:
     * `appDevUrlGenerator.php`, `url_generating_routes.php`, `UrlGenerator.php`, etc.
     *
     * Callers are expected to pre-filter paths with [isCachePath] before invoking this.
     */
    fun isCompiledRoutePath(path: String): Boolean {
        val normalized = normalizePath(path)
        if (!normalized.endsWith(".php")) return false
        val filename = normalized.lowercase().substringAfterLast('/').replace("_", "")
        return filename.contains("urlgenerator") || filename.contains("urlgenerating")
    }

    /**
     * Returns true for compiled Symfony container XML files inside `dev` or `dev_*` cache directories.
     *
     * Callers are expected to pre-filter paths with [isCachePath] before invoking this.
     */
    fun isCompiledContainerPath(path: String): Boolean {
        val normalized = normalizePath(path)
        if (!normalized.endsWith(".xml")) return false

        val lowered = normalized.lowercase()
        // `contains` intentionally matches both `dev` and `dev_*` (e.g. `dev_392373729`) cache directories.
        if (!sequenceOf("/var/cache/dev", "/app/cache/dev").any { lowered.contains(it) }) return false

        val filename = lowered.substringAfterLast('/')
        return filename.contains("debugcontainer")
            || (filename.contains("debug") && filename.contains("container"))
            || (filename.contains("kernel") && filename.contains("container"))
    }

    /**
     * Returns true when [path] is inside [root] or when [path] is a parent of [root].
     *
     * The second case is needed for rename/delete events affecting a watched directory
     * through one of its ancestors.
     */
    fun isRelevantPath(path: String, root: String): Boolean {
        val normalizedPath = normalizePath(path)
        val normalizedRoot = normalizePath(root)

        return FileUtil.startsWith(normalizedPath, normalizedRoot) || FileUtil.startsWith(normalizedRoot, normalizedPath)
    }

    /**
     * Normalizes a directory-like root and ensures it ends with `/`.
     */
    fun normalizeRootPath(path: String): String {
        val normalized = normalizePath(path)
        return if (normalized.endsWith("/")) normalized else "$normalized/"
    }

    /**
     * Cross-platform absolute-path detection for normalized VFS-style paths.
     *
     * Supports Unix roots, UNC-style roots, and Windows drive-letter paths independently
     * of the host OS running the IDE or the tests.
     */
    fun isAbsolutePath(path: String): Boolean {
        val normalized = normalizePath(path)
        return normalized.startsWith("/")
            || normalized.startsWith("//")
            || (normalized.length >= 3 && normalized[1] == ':' && normalized[2] == '/')
    }

    /**
     * Converts a path to a canonical VFS-style form.
     *
     * This normalizes separators, resolves `.` and `..`, and folds Windows drive letters
     * to lowercase so string-based matching remains stable across event sources.
     */
    fun normalizePath(path: String): String {
        val canonical = FileUtil.toCanonicalPath(FileUtil.toSystemIndependentName(path))

        if (canonical.length >= 3 && canonical[1] == ':' && canonical[2] == '/') {
            return canonical[0].lowercaseChar() + canonical.substring(1)
        }

        return canonical
    }
}
