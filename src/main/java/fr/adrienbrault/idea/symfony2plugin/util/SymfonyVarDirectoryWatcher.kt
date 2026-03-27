package fr.adrienbrault.idea.symfony2plugin.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.util.Alarm
import com.intellij.util.messages.MessageBusConnection
import fr.adrienbrault.idea.symfony2plugin.Settings
import java.util.Collections
import java.util.EnumSet

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

    /**
     * Stable trackers that consumers can keep as dependencies.
     */
    private val trackers: Map<Scope, SimpleModificationTracker> = mapOf(
        Scope.CONTAINER to SimpleModificationTracker(),
        Scope.TRANSLATIONS to SimpleModificationTracker(),
        Scope.ROUTES to SimpleModificationTracker(),
    )

    /** Active VFS listener connection while the plugin is enabled for the project. */
    @Volatile
    private var listenerConnection: MessageBusConnection? = null

    private val debounceAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)
    private val pendingScopes: MutableSet<Scope> = Collections.synchronizedSet(EnumSet.noneOf(Scope::class.java))

    init {
        refreshSubscription()
    }

    override fun dispose() {
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
                        .filter(SymfonyVarDirectoryWatchScopeUtil::isCachePath)

                    if (paths.isEmpty()) continue

                    val matchedScopes = SymfonyVarDirectoryWatchScopeUtil.collectScopesForPaths(scopes, paths)
                    matchedScopes.forEach { scope ->
                        toInvalidate += scope
                    }
                }

                // Debounce: accumulate matched scopes and reschedule the flush so rapid
                // bursts of VFS events (e.g. a Symfony cache warm-up) produce a single invalidation.
                if (toInvalidate.isNotEmpty()) {
                    pendingScopes.addAll(toInvalidate)
                    debounceAlarm.cancelAllRequests()
                    debounceAlarm.addRequest(::flushPendingInvalidations, 300)
                }
            }
        })

        return connection
    }

    /**
     * Drains [pendingScopes] and fires one [invalidate] call per scope.
     */
    private fun flushPendingInvalidations() {
        val scopes: Set<Scope>
        synchronized(pendingScopes) {
            if (pendingScopes.isEmpty()) return
            scopes = EnumSet.copyOf(pendingScopes)
            pendingScopes.clear()
        }
        scopes.forEach { invalidate(it) }
    }

    private fun refreshSubscription() {
        debounceAlarm.cancelAllRequests()
        listenerConnection?.disconnect()
        listenerConnection = null

        if (!Settings.getInstance(project).pluginEnabled) {
            return
        }

        listenerConnection = subscribe(detectRoots())
    }

    private fun detectRoots(): List<SymfonyVarDirectoryWatchScopeUtil.ScopeEntry> {
        val projectDir = ProjectUtil.getProjectDir(project) ?: return emptyList()
        return SymfonyVarDirectoryWatchScopeUtil.createScopes(projectDir.path, Settings.getInstance(project))
    }

    private fun invalidate(scope: Scope) {
        trackers.getValue(scope).incModificationCount()
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
