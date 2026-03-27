package fr.adrienbrault.idea.symfony2plugin.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.util.messages.MessageBusConnection
import fr.adrienbrault.idea.symfony2plugin.Settings
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory

/**
 * Watches Symfony cache-related paths and exposes per-scope invalidation trackers.
 *
 * Initialized on project startup and refreshed on Symfony settings changes.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@Service(Service.Level.PROJECT)
class SymfonyVarDirectoryWatcher(private val project: Project) : Disposable {

    /**
     * Cache invalidation scopes for compiled Symfony artifacts.
     */
    enum class Scope {
        CONTAINER,
        TRANSLATIONS,
        ROUTES,
    }

    companion object {
        private val LOG = Logger.getInstance(SymfonyVarDirectoryWatcher::class.java)
    }

    /** Temporary verbose logging for watcher verification. */
    private val debug = true

    /** Stable trackers that consumers can keep as dependencies. */
    private val trackers: Map<Scope, SimpleModificationTracker> = mapOf(
        Scope.CONTAINER    to SimpleModificationTracker(),
        Scope.TRANSLATIONS to SimpleModificationTracker(),
        Scope.ROUTES       to SimpleModificationTracker(),
    )

    /** Active VFS listener connection while the plugin is enabled for the project. */
    @Volatile
    private var listenerConnection: MessageBusConnection? = null

    init {
        refreshSubscription()
    }

    override fun dispose() {
        debugLog("disposed for project ${project.name}")
    }

    /**
     * Returns the tracker for the given invalidation scope.
     * */
    fun getModificationTracker(scope: Scope): SimpleModificationTracker =
        trackers.getValue(scope)

    /**
     * Rebuilds the watcher subscription from current settings and invalidates all scopes.
     */
    fun reloadConfiguration() {
        refreshSubscription()
        trackers.keys.forEach { invalidate(it) }
    }

    /**
     *  Subscribes a VFS listener for the provided scope snapshot.
     */
    private fun subscribe(scopes: List<SymfonyVarDirectoryWatchScopeUtil.ScopeEntry>): MessageBusConnection {
        debugLog("subscribe() " + scopes.joinToString(", ") { "${it.scope}=${it.roots}" })

        val connection = project.messageBus.connect(this)
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                val toInvalidate = mutableSetOf<Scope>()

                for (event in events) {
                    // Rename events need both old and new names to catch moves in either direction.
                    val paths = if (event is VFilePropertyChangeEvent && event.propertyName == VirtualFile.PROP_NAME) {
                        val parent = event.file.parent?.path ?: ""
                        listOf(event.path, "$parent/${event.oldValue}")
                    } else {
                        listOf(event.path)
                    }.map(SymfonyVarDirectoryWatchScopeUtil::normalizePath)

                    val matchedScopes = SymfonyVarDirectoryWatchScopeUtil.collectScopesForPaths(scopes, paths)
                    matchedScopes.forEach { scope ->
                        if (scope !in toInvalidate) {
                            debugLog("VFS event: ${eventTypeName(event)} $paths → scope: $scope")
                            toInvalidate += scope
                        }
                    }
                }

                for (scope in toInvalidate) {
                    invalidate(scope)
                }
            }
        })

        return connection
    }

    private fun refreshSubscription() {
        listenerConnection?.disconnect()
        listenerConnection = null

        if (!Settings.getInstance(project).pluginEnabled) {
            debugLog("Watcher disabled for project ${project.name}")
            return
        }

        listenerConnection = subscribe(detectRoots())
    }

    /**
     *  Detects watch roots and touches cache directories so VFS receives external changes.
     */
    private fun detectRoots(): List<SymfonyVarDirectoryWatchScopeUtil.ScopeEntry> {
        val projectDir = ProjectUtil.getProjectDir(project) ?: return emptyList()

        // Register cache roots so external filesystem changes reach the VFS listener.
        for (cacheRoot in listOf("var/cache", "app/cache")) {
            VfsUtil.findRelativeFile(cacheRoot, projectDir)
        }

        val scopes = SymfonyVarDirectoryWatchScopeUtil.createScopes(projectDir.path, Settings.getInstance(project))

        LOG.info("[SymfonyVarDirectoryWatcher] detectRoots() " +
                 scopes.joinToString(", ") { "${it.scope}=${it.roots}" })

        return scopes
    }

    private fun invalidate(scope: Scope) {
        val tracker = trackers.getValue(scope)
        val before = tracker.modificationCount
        tracker.incModificationCount()
        val after = tracker.modificationCount

        debugLog("  → [$scope] tracker: $before → $after")

        // Compat bridge until container consumers depend directly on the watcher tracker.
        if (scope == Scope.CONTAINER) {
            ServiceXmlParserFactory.cleanInstance(project)
            debugLog("  → ServiceXmlParserFactory.cleanInstance() called")
        }

        LOG.info("[SymfonyVarDirectoryWatcher] [$scope] cache invalidated (tracker: $after)")
    }

    private fun eventTypeName(event: VFileEvent): String =
        event.javaClass.simpleName.removePrefix("VFile").removeSuffix("Event").uppercase()

    private fun debugLog(message: String) {
        if (!debug) return
        val full = "[SymfonyVarDirectoryWatcher] $message"
        println(full)
        LOG.warn(full)
    }
}

/**
 *  Retrieves the [SymfonyVarDirectoryWatcher] project service.
 */
fun getSymfonyVarDirectoryWatcher(project: Project): SymfonyVarDirectoryWatcher =
    project.getService(SymfonyVarDirectoryWatcher::class.java)

/**
 *  Syncs watcher lifecycle with the current project-level plugin enabled setting.
 */
fun syncSymfonyVarDirectoryWatcher(project: Project) {
    if (Settings.getInstance(project).pluginEnabled) {
        getSymfonyVarDirectoryWatcher(project).reloadConfiguration()
        return
    }

    project.getServiceIfCreated(SymfonyVarDirectoryWatcher::class.java)?.reloadConfiguration()
}
