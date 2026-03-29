package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.InvalidVirtualFileAccessException;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringMap;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyVarDirectoryWatcher;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyVarDirectoryWatcherKt;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationIndex {
    private static final Key<CachedValue<TranslationStringMap>> SYMFONY_TRANSLATION_MAP_COMPILED = new Key<>("SYMFONY_TRANSLATION_MAP_COMPILED");

    private TranslationIndex() {
    }

    synchronized static public TranslationStringMap getTranslationMap(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            SYMFONY_TRANSLATION_MAP_COMPILED,
            () -> {
                Collection<VirtualFile> translationDirectories = findTranslationDirectories(project);

                TranslationStringMap translationStringMap;
                if (!translationDirectories.isEmpty()) {
                    translationStringMap = TranslationStringMap.create(translationDirectories);
                } else {
                    translationStringMap = TranslationStringMap.createEmpty();
                }

                return CachedValueProvider.Result.create(translationStringMap, getTranslationsTracker(project));
            },
            false
        );
    }

    /**
     * Discovers compiled translation catalogue directories from var/cache and app/cache
     * "dev" directories, the configured pathToTranslation setting, and sibling translations/
     * directories next to container XML files.
     *
     * Cache invalidation is handled by SymfonyVarDirectoryWatcher via VFS events.
     */
    @NotNull
    private static Collection<VirtualFile> findTranslationDirectories(@NotNull Project project) {
        Collection<VirtualFile> files = new HashSet<>();

        VirtualFile projectDir = ProjectUtil.getProjectDir(project);
        if (projectDir != null) {
            for (String root : new String[] {"var/cache", "app/cache"}) {
                VirtualFile cache;
                try {
                    cache = VfsUtil.findRelativeFile(projectDir, root.split("/"));
                } catch (InvalidVirtualFileAccessException ignored) {
                    continue;
                }

                if (cache == null) {
                    continue;
                }

                for (VirtualFile child : cache.getChildren()) {
                    String filename = child.getName();
                    // support "dev" and "dev_*"
                    if (!"dev".equals(filename) && !filename.startsWith("dev_")) {
                        continue;
                    }

                    VirtualFile translations = child.findChild("translations");
                    if (translations == null) {
                        continue;
                    }

                    files.add(translations);
                }
            }

            String translationPath = Settings.getInstance(project).pathToTranslation;
            if (StringUtils.isNotBlank(translationPath)) {
                VirtualFile vf;
                if (FileUtil.isAbsolute(translationPath)) {
                    vf = VfsUtil.findFileByIoFile(new java.io.File(translationPath), false);
                } else {
                    vf = VfsUtil.findRelativeFile(projectDir, translationPath.replace('\\', '/').split("/"));
                }

                if (vf != null && vf.isDirectory()) {
                    files.add(vf);
                }
            }
        }

        for (String containerFile : ServiceContainerUtil.getContainerFiles(project)) {
            // resolve the file
            VirtualFile containerVirtualFile = VfsUtil.findRelativeFile(containerFile, projectDir);
            if (containerVirtualFile == null) {
                continue;
            }

            // get directory of the file; translation folder is same directory
            VirtualFile cacheDirectory = containerVirtualFile.getParent();
            if (cacheDirectory == null) {
                continue;
            }

            // get translation sub directory
            VirtualFile translations = cacheDirectory.findChild("translations");
            if (translations != null) {
                files.add(translations);
            }
        }

        return files;
    }

    private static SimpleModificationTracker getTranslationsTracker(@NotNull Project project) {
        return SymfonyVarDirectoryWatcherKt.getSymfonyVarDirectoryWatcher(project)
            .getModificationTracker(SymfonyVarDirectoryWatcher.Scope.TRANSLATIONS);
    }
}
