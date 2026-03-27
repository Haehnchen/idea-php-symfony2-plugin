# Plan: Symfony `var/` Directory VFS Watcher & Cache Invalidation

## Problem Statement

### Current Situation

The plugin reads compiled Symfony artifacts from `var/cache/dev/` (container XML, translations, routes, etc.) that are **outside the IntelliJ project index** (typically excluded from indexing). Several layers of caching exist:

1. **`ServiceXmlParserFactory`** – a hand-rolled pull-based cache: on every access it compares `VirtualFile.getTimeStamp()` for each tracked file and re-parses if any timestamp changed. This works but runs on every call.
2. **`AbsoluteFileModificationTracker`** – a `ModificationTracker` that sums `VirtualFile.getTimeStamp()` across a set of absolute paths. Used in `CachedValue` dependency chains.
3. **`TimeSecondModificationTracker`** – a 60-second TTL that forces cache expiry regardless of file changes. Used in `TranslationIndex` as a coarse fallback.
4. **`TranslationIndex.getTranslationRoot()`** – the most sophisticated example: nested `CachedValue` calls, with `TranslationSettingsModificationTracker` (reacts to settings changes) and `AbsoluteFileModificationTracker` (reacts to directory timestamps).

### Core Problems

- **Pull-based / polling**: Caches are invalidated lazily on the next call, not pushed from file system events. The 60-second TTL means stale data after `bin/console cache:clear`.
- **No event-driven invalidation**: When the user runs `cache:clear`, the container XML is deleted and recreated. The plugin only notices on the next polling cycle.
- **Scattered invalidation logic**: Every consumer (`TranslationIndex`, `ServiceContainerUtil`, `ServiceXmlParserFactory`) has its own staleness-detection copy-pasted approach.
- **Settings changes not unified**: Each tracker independently re-checks settings; there is no single "settings changed → invalidate everything" signal.
- **`var/` is not indexed**: Standard `FileBasedIndex` change events don't fire for `var/cache/`. Only low-level VFS listeners (below the indexer) see these changes.

---

## Goals

1. **Event-driven invalidation** for files under `var/` (and `app/cache/`) using IntelliJ's VFS `BulkFileListener`.
2. **Single source of truth** – one `ModificationTracker` that all caches can depend on.
3. **Settings awareness** – re-detect the `var/` root and increment the tracker when plugin settings change.
4. **Force-reset API** – callable from tests and from the `cleanInstance` code path.
5. **Debug mode with console logging** – so we can verify correct event flow during development.
6. **Long-term: replace `ServiceXmlParserFactory`** with `CachedValue`-based caches that depend on this tracker.

---

## Solution Design

### Architecture Overview

```
IntelliJ VFS events (BulkFileListener)
         │
         ▼
SymfonyVarDirectoryWatcher   ◄──── Settings change signal
  (project service, Disposable)
         │
         ├── VarDirectoryModificationTracker  (SimpleModificationTracker)
         │         used as CachedValue dependency
         │
         └── calls ServiceXmlParserFactory.cleanInstance(project)
                   (immediate compat bridge)
```

### Component: `SymfonyVarDirectoryWatcher`

**Package:** `fr.adrienbrault.idea.symfony2plugin.util`
**Type:** Project-level service (`projectService`)
**Implements:** `Disposable`

#### Responsibilities

1. **Root detection** (once per settings-change cycle):
   - Scan `var/cache` and `app/cache` relative to `ProjectUtil.getProjectDir()`.
   - Also include `Settings.getInstance(project).pathToTranslation` parent directory.
   - Also include container files from `ServiceContainerUtil.getContainerFiles()`.
   - Cache the resulting set of watched root paths in a field.
   - Re-detect roots when the `VarDirectoryModificationTracker` (settings-aware) fires.

2. **VFS subscription** (registered once at init, disposed on project close):
   - Subscribe to `VirtualFileManager.VFS_CHANGES` (`BulkFileListener`) on `project.getMessageBus()`.
   - For each `VFileEvent`, check if the affected path **starts with** any watched root.
   - If yes → call `invalidate()`.

3. **Settings change detection**:
   - Maintain an inner `SettingsModificationTracker` (like `TranslationSettingsModificationTracker`) hashing the container file paths and `pathToTranslation` setting.
   - Check it on each VFS batch; if changed → re-detect roots, then invalidate.

4. **`invalidate()` method**:
   - Increment the public `VarDirectoryModificationTracker`.
   - Call `ServiceXmlParserFactory.cleanInstance(project)` (compat bridge, to be removed later).
   - Log to console (DEBUG) with the triggering path.

5. **`forceReset()` method**:
   - Re-detect roots + call `invalidate()`.
   - Usable from tests and from a "Reset Plugin Caches" action.

#### API

```java
public class SymfonyVarDirectoryWatcher implements Disposable {
    // Access the singleton for a project
    public static SymfonyVarDirectoryWatcher getInstance(@NotNull Project project);

    // ModificationTracker for CachedValue dependencies
    public ModificationTracker getModificationTracker();

    // Force full reset (re-detect roots + invalidate)
    public void forceReset();

    // For testing: get currently watched roots
    public Set<String> getWatchedRoots();
}
```

### Component: `VarDirectoryModificationTracker`

```java
// inner class or standalone
class VarDirectoryModificationTracker extends SimpleModificationTracker {
    // incModificationCount() called by SymfonyVarDirectoryWatcher.invalidate()
}
```

### Registration in `plugin.xml`

```xml
<projectService serviceImplementation="fr.adrienbrault.idea.symfony2plugin.util.SymfonyVarDirectoryWatcher"/>
```

No `<listeners>` block needed — the service self-registers via `project.getMessageBus().connect(this)` in its constructor.

---

## Implementation Steps

### Step 1 – Skeleton with debug logging (current task)

Create `SymfonyVarDirectoryWatcher` with:
- Root detection (scan `var/cache`, `app/cache`).
- `BulkFileListener` subscription.
- Console `System.out.println` / `LOG.warn` debug output on every detected event.
- `VarDirectoryModificationTracker` that increments on each event.
- `ServiceXmlParserFactory.cleanInstance()` bridge call.
- Register as project service.

**Goal:** Deploy to a real Symfony project, run `bin/console cache:clear`, and verify in IDE log / console that events fire.

### Step 2 – Settings-change detection

Add `SettingsModificationTracker` that monitors container file paths and `pathToTranslation`. Check it on each VFS batch (cheap hash comparison). If changed → re-detect roots.

### Step 3 – Replace `TimeSecondModificationTracker` in `TranslationIndex`

Replace the 60-second TTL with a dependency on `SymfonyVarDirectoryWatcher.getModificationTracker()`. The translation cache will now be invalidated exactly when `var/` changes, not on a timer.

### Step 4 – Replace `AbsoluteFileModificationTracker` in `ServiceContainerUtil`

`getContainerFilesInner()` currently depends on `AbsoluteFileModificationTracker`. Replace with `SymfonyVarDirectoryWatcher.getModificationTracker()`.

### Step 5 – Migrate `ServiceXmlParserFactory` to `CachedValue`

Convert `ServiceXmlParserFactory` from a manual timestamp-comparison cache to a `CachedValue` with `SymfonyVarDirectoryWatcher.getModificationTracker()` as dependency. This eliminates per-call timestamp polling.

---

## Key IntelliJ APIs

| Purpose | API |
|---|---|
| VFS events (incl. outside project) | `project.getMessageBus().connect(disposable).subscribe(VirtualFileManager.VFS_CHANGES, bulkFileListener)` |
| Check if path is under watched root | `path.startsWith(root)` or `VfsUtil.isAncestor()` |
| Project-level service | `@Service(Service.Level.PROJECT)` + plugin.xml `<projectService>` |
| Mod tracker for CachedValue | `SimpleModificationTracker` |
| Project base dir | `ProjectUtil.getProjectDir(project)` |
| Relative VFS lookup | `VfsUtil.findRelativeFile(root, baseDir)` |
| Settings | `Settings.getInstance(project)` |

### Why `BulkFileListener` works for `var/`

The IntelliJ VFS is a thin caching layer over the native FS, and `BulkFileListener` fires for **all VFS events**, regardless of whether the file is in the project index or excluded. The indexer exclusion only prevents `FileBasedIndex` from indexing the content — VFS events still propagate. This is the same mechanism used by IntelliJ's "External File Changes" detection.

> **Note:** VFS events only fire if IntelliJ has previously "seen" the directory (i.e., `VfsUtil.findRelativeFile()` was called on it, causing VFS to track it). The root detection step (which calls `VfsUtil.findRelativeFile()` on `var/cache`) implicitly registers the directory with VFS, ensuring subsequent changes are observed.

---

## What the Debug Skeleton Should Output

When a file under `var/cache/` changes, the console/log should show:

```
[SymfonyVarDirectoryWatcher] VFS event: CHANGED /path/to/project/var/cache/dev/App_KernelDevDebugContainer.xml
[SymfonyVarDirectoryWatcher]   → matches watched root: /path/to/project/var/cache
[SymfonyVarDirectoryWatcher]   → invalidating caches (tracker count: 1 → 2)
[SymfonyVarDirectoryWatcher]   → ServiceXmlParserFactory.cleanInstance() called
```

When `cache:clear` runs (bulk delete + recreate):
```
[SymfonyVarDirectoryWatcher] VFS event: DELETED /path/to/project/var/cache/dev/App_KernelDevDebugContainer.xml
[SymfonyVarDirectoryWatcher]   → matches watched root: /path/to/project/var/cache
[SymfonyVarDirectoryWatcher]   → invalidating caches (tracker count: 2 → 3)
...
[SymfonyVarDirectoryWatcher] VFS event: CREATED /path/to/project/var/cache/dev/App_KernelDevDebugContainer.xml
[SymfonyVarDirectoryWatcher]   → matches watched root: /path/to/project/var/cache
[SymfonyVarDirectoryWatcher]   → invalidating caches (tracker count: 3 → 4)
```

---

## Open Questions / Edge Cases

1. **VFS refresh timing**: IntelliJ's VFS doesn't always pick up external changes immediately. External file changes (from CLI) trigger a VFS refresh. The `BulkFileListener` fires _after_ the refresh. In practice, the IDE detects external changes within a few seconds via its background VFS refresh thread — no explicit refresh call needed.
2. **Debouncing**: `cache:clear` can fire dozens of VFS events (delete every cache file, recreate). Should we debounce? For the debug skeleton: no debouncing — log every event. For production: debounce or batch (e.g., coalesce events within 500ms). `ServiceXmlParserFactory` already handles "invalid" state gracefully, so multiple invalidations are safe but wasteful.
3. **`var/` not yet existing**: If the project has never been warmed up (`var/cache/` doesn't exist), root detection returns empty. First `cache:warmup` creates the directory — but VFS won't know about it until it refreshes. A periodic fallback (maybe 60s `TimeSecondModificationTracker` alongside) could help cold starts.
4. **Multiple content roots / monorepos**: `ProjectUtil.getProjectDir()` returns the project dir. For monorepos with multiple Symfony apps, we may need to scan multiple roots. Out of scope for now.
