package fr.adrienbrault.idea.symfony2plugin.tests.util

import fr.adrienbrault.idea.symfony2plugin.Settings
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile
import fr.adrienbrault.idea.symfony2plugin.routing.dict.RoutingFile
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyVarDirectoryWatchScopeUtil
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyVarDirectoryWatcher
import junit.framework.TestCase

class SymfonyVarDirectoryWatchScopeUtilTest : TestCase() {
    fun testNormalizePathResolvesRelativeSegments() {
        assertEquals(
            "/project/var/cache/dev/translations",
            SymfonyVarDirectoryWatchScopeUtil.normalizePath("/project/var/cache/../cache/dev/./translations")
        )
    }

    fun testNormalizeRootPathAppendsTrailingSlash() {
        assertEquals(
            "/project/var/cache/dev/translations/",
            SymfonyVarDirectoryWatchScopeUtil.normalizeRootPath("/project/var/cache/dev/translations")
        )
    }

    fun testIsAbsolutePathSupportsUnixAndRelativePaths() {
        assertTrue(SymfonyVarDirectoryWatchScopeUtil.isAbsolutePath("/project/var/cache/dev"))
        assertFalse(SymfonyVarDirectoryWatchScopeUtil.isAbsolutePath("./var/cache/dev"))
    }

    fun testNormalizePathSupportsWindowsDriveLetterCase() {
        assertEquals(
            "c:/project/var/cache/dev",
            SymfonyVarDirectoryWatchScopeUtil.normalizePath("C:\\project\\var\\cache\\dev")
        )
    }

    fun testIsAbsolutePathSupportsWindowsPaths() {
        assertTrue(SymfonyVarDirectoryWatchScopeUtil.isAbsolutePath("C:\\project\\var\\cache\\dev"))
    }

    fun testIsRelevantPathMatchesInsideRootOnUnixStylePaths() {
        assertTrue(
            SymfonyVarDirectoryWatchScopeUtil.isRelevantPath(
                "/project/var/cache/dev/App_KernelDevDebugContainer.xml",
                "/project/var/cache/"
            )
        )
    }

    fun testIsRelevantPathMatchesInsideRootOnWindowsStylePaths() {
        assertTrue(
            SymfonyVarDirectoryWatchScopeUtil.isRelevantPath(
                "C:\\project\\var\\cache\\dev\\App_KernelDevDebugContainer.xml",
                "c:/project/var/cache/"
            )
        )
    }

    fun testIsCompiledContainerPathMatchesDevXmlContainerFile() {
        assertTrue(
            SymfonyVarDirectoryWatchScopeUtil.isCompiledContainerPath(
                "/project/var/cache/dev/App_KernelDevDebugContainer.xml"
            )
        )
    }

    fun testIsCompiledContainerPathMatchesDevUnderscoreDirectory() {
        assertTrue(
            SymfonyVarDirectoryWatchScopeUtil.isCompiledContainerPath(
                "/project/app/cache/dev_392373729/appDevDebugProjectContainer.xml"
            )
        )
    }

    fun testIsCompiledContainerPathRejectsNonContainerXml() {
        assertFalse(
            SymfonyVarDirectoryWatchScopeUtil.isCompiledContainerPath(
                "/project/var/cache/dev/services.xml"
            )
        )
    }

    fun testIsCompiledContainerPathRejectsProdCacheXml() {
        assertFalse(
            SymfonyVarDirectoryWatchScopeUtil.isCompiledContainerPath(
                "/project/var/cache/prod/App_KernelProdContainer.xml"
            )
        )
    }

    fun testIsCompiledTranslationCachePathMatchesDevTranslationsFile() {
        assertTrue(
            SymfonyVarDirectoryWatchScopeUtil.isCompiledTranslationCachePath(
                "/project/var/cache/dev/translations/catalogue.en.php"
            )
        )
    }

    fun testIsCompiledTranslationCachePathMatchesDevUnderscoreDirectory() {
        assertTrue(
            SymfonyVarDirectoryWatchScopeUtil.isCompiledTranslationCachePath(
                "/project/app/cache/dev_392373729/translations/catalogue.en.php"
            )
        )
    }

    fun testIsCompiledTranslationCachePathMatchesDevDirectoryParent() {
        assertTrue(
            SymfonyVarDirectoryWatchScopeUtil.isCompiledTranslationCachePath(
                "/project/var/cache/dev"
            )
        )
    }

    fun testIsCompiledTranslationCachePathRejectsNonTranslationCacheFile() {
        assertFalse(
            SymfonyVarDirectoryWatchScopeUtil.isCompiledTranslationCachePath(
                "/project/var/cache/dev/services.xml"
            )
        )
    }

    fun testCreateScopesNormalizesConfiguredRoots() {
        val settings = Settings().apply {
            pathToTranslation = "./var/cache/dev/translations"
            containerFiles = arrayListOf(ContainerFile("/project/var/cache/dev/App_KernelDevDebugContainer.xml"))
            routingFiles = arrayListOf(RoutingFile("./var/cache/dev/url_generating_routes.php"))
        }

        val scopes = SymfonyVarDirectoryWatchScopeUtil.createScopes("/project", settings)
            .associateBy { it.scope }

        assertTrue(scopes.getValue(SymfonyVarDirectoryWatcher.Scope.TRANSLATIONS).roots.contains("/project/var/cache/dev/translations/"))
        assertTrue(scopes.getValue(SymfonyVarDirectoryWatcher.Scope.TRANSLATIONS).roots.contains("/project/var/cache/"))
        assertTrue(scopes.getValue(SymfonyVarDirectoryWatcher.Scope.TRANSLATIONS).roots.contains("/project/var/cache/dev/translations/"))
        assertTrue(scopes.getValue(SymfonyVarDirectoryWatcher.Scope.CONTAINER).roots.contains("/project/var/cache/dev/App_KernelDevDebugContainer.xml"))
        assertTrue(scopes.getValue(SymfonyVarDirectoryWatcher.Scope.ROUTES).roots.contains("/project/var/cache/dev/url_generating_routes.php"))
    }

    fun testCollectScopesForPathsMatchesContainerXmlOnly() {
        val scopes = SymfonyVarDirectoryWatchScopeUtil.createScopes("/project", Settings())

        val matched = SymfonyVarDirectoryWatchScopeUtil.collectScopesForPaths(
            scopes,
            listOf("/project/var/cache/dev/App_KernelDevDebugContainer.xml")
        )

        assertEquals(setOf(SymfonyVarDirectoryWatcher.Scope.CONTAINER), matched)
    }

    fun testCollectScopesForPathsMatchesTranslationsDirectoryRenameFromParentPath() {
        val scopes = SymfonyVarDirectoryWatchScopeUtil.createScopes("/project", Settings())

        val matched = SymfonyVarDirectoryWatchScopeUtil.collectScopesForPaths(
            scopes,
            listOf("/project/var/cache/dev")
        )

        assertTrue(matched.contains(SymfonyVarDirectoryWatcher.Scope.TRANSLATIONS))
    }

    fun testCollectScopesForPathsExcludesTranslationPhpFromRoutes() {
        val scopes = SymfonyVarDirectoryWatchScopeUtil.createScopes("/project", Settings())

        val matched = SymfonyVarDirectoryWatchScopeUtil.collectScopesForPaths(
            scopes,
            listOf("/project/var/cache/dev/translations/catalogue.en.php")
        )

        assertFalse(matched.contains(SymfonyVarDirectoryWatcher.Scope.ROUTES))
        assertTrue(matched.contains(SymfonyVarDirectoryWatcher.Scope.TRANSLATIONS))
    }

    fun testCollectScopesForPathsMatchesDevUnderscoreTranslationsPhp() {
        val scopes = SymfonyVarDirectoryWatchScopeUtil.createScopes("/project", Settings())

        val matched = SymfonyVarDirectoryWatchScopeUtil.collectScopesForPaths(
            scopes,
            listOf("/project/var/cache/dev_123/translations/catalogue.en.php")
        )

        assertTrue(matched.contains(SymfonyVarDirectoryWatcher.Scope.TRANSLATIONS))
    }

    fun testCollectScopesForPathsMatchesDefaultCompiledRoutePhp() {
        val scopes = SymfonyVarDirectoryWatchScopeUtil.createScopes("/project", Settings())

        val matched = SymfonyVarDirectoryWatchScopeUtil.collectScopesForPaths(
            scopes,
            listOf("/project/var/cache/dev/url_generating_routes.php")
        )

        assertTrue(matched.contains(SymfonyVarDirectoryWatcher.Scope.ROUTES))
    }
}
