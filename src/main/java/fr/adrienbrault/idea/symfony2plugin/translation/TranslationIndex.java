package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringMap;
import fr.adrienbrault.idea.symfony2plugin.util.AbsoluteFileModificationTracker;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import fr.adrienbrault.idea.symfony2plugin.util.TimeSecondModificationTracker;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationIndex {
    private static final Key<CachedValue<Collection<File>>> SYMFONY_TRANSLATION_COMPILED_TIMED_WATCHER = new Key<>("SYMFONY_TRANSLATION_COMPILED_TIMED_WATCHER");
    private static final Key<CachedValue<Collection<File>>> SYMFONY_TRANSLATION_COMPILED = new Key<>("SYMFONY_TRANSLATION_COMPILED");
    private static final Key<CachedValue<Collection<String>>> SYMFONY_GUESTED_TRANSLATION_DIRECTORIES = new Key<>("SYMFONY_GUESTED_TRANSLATION_DIRECTORIES");

    private TranslationIndex() {
    }

    private static final Key<CachedValue<TranslationStringMap>> SYMFONY_TRANSLATION_MAP_COMPILED = new Key<>("SYMFONY_TRANSLATION_MAP_COMPILED");

    synchronized static public TranslationStringMap getTranslationMap(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            SYMFONY_TRANSLATION_MAP_COMPILED,
            () -> {
                Collection<File> translationDirectories = getTranslationRoot(project);

                TranslationStringMap translationStringMap;
                if (translationDirectories.size() > 0) {
                    translationStringMap = TranslationStringMap.create(project, translationDirectories);
                } else {
                    translationStringMap = TranslationStringMap.createEmpty();
                }

                Symfony2ProjectComponent.getLogger().info("translations changed: " + StringUtils.join(translationDirectories.stream().map(File::toString).collect(Collectors.toSet()), ","));

                return CachedValueProvider.Result.create(translationStringMap, new FileModificationModificationTracker(project));
            },
            false
        );
    }

    @NotNull
    private static Collection<File> getTranslationRoot(@NotNull Project project) {
        return CachedValuesManager.getManager(project)
            .getCachedValue(
                project,
                SYMFONY_TRANSLATION_COMPILED,
                () -> CachedValueProvider.Result.create(getTranslationRootInnerTime(project), PsiModificationTracker.MODIFICATION_COUNT),
                false
            );
    }

    @NotNull
    private static Collection<File> getTranslationRootInnerTime(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            SYMFONY_TRANSLATION_COMPILED_TIMED_WATCHER,
            () -> CachedValueProvider.Result.create(getTranslationRootInner(project), TimeSecondModificationTracker.TIMED_MODIFICATION_TRACKER_60),
            false
        );
    }

    private static class TranslationSettingsModificationTracker extends SimpleModificationTracker {
        private final @NotNull Project project;
        private int last = 0;

        public TranslationSettingsModificationTracker(@NotNull Project project) {
            this.project = project;
        }

        @Override
        public long getModificationCount() {
            String pathToTranslation = Settings.getInstance(project).pathToTranslation;
            if (StringUtils.isBlank(pathToTranslation)) {
                pathToTranslation = "";
            }

            int hash = pathToTranslation.hashCode();
            if (hash != this.last) {
                this.last = hash;
                this.incModificationCount();
            }

            return super.getModificationCount();
        }
    }

    @NotNull
    private static Collection<File> getTranslationRootInner(@NotNull Project project) {
        Collection<File> files = new HashSet<>();
        Collection<String> filesAbsolute = new HashSet<>();

        VirtualFile projectDir = ProjectUtil.getProjectDir(project);
        if (projectDir != null) {
            Collection<String> cachedValue = CachedValuesManager.getManager(project).getCachedValue(
                project,
                SYMFONY_GUESTED_TRANSLATION_DIRECTORIES,
                () -> {
                    Set<String> directories = new HashSet<>();
                    Set<String> caches = new HashSet<>();

                    for (String root : new String[] {"var/cache", "app/cache"}) {
                        VirtualFile cache = VfsUtil.findRelativeFile(projectDir, root.split("/"));
                        if (cache == null) {
                            continue;
                        }

                        caches.add(cache.getParent().getPath());

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

                            directories.add(translations.getPath());
                        }
                    }

                    String translationPath = Settings.getInstance(project).pathToTranslation;
                    if (StringUtils.isNotBlank(translationPath)) {
                        if (!FileUtil.isAbsolute(translationPath)) {
                            translationPath = project.getBasePath() + "/" + translationPath;
                        }

                        File file = new File(translationPath);
                        if(file.exists() && file.isDirectory()) {
                            directories.add(file.getPath());
                        }
                    }

                    caches.addAll(directories);

                    return CachedValueProvider.Result.create(
                        Collections.unmodifiableSet(directories),
                        new TranslationSettingsModificationTracker(project),
                        new AbsoluteFileModificationTracker(caches)
                    );
                },
                false
            );

            files.addAll(cachedValue.stream().map(File::new).collect(Collectors.toSet()));
            filesAbsolute.addAll(cachedValue);
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
            if (translations != null && !filesAbsolute.contains(translations.getPath())) {
                filesAbsolute.add(translations.getPath());
                files.add(VfsUtilCore.virtualToIoFile(translations));
            }
        }

        return files;
    }

    private static class FileModificationModificationTracker extends SimpleModificationTracker {
        @NotNull
        private final Project project;
        private long last = 0;

        public FileModificationModificationTracker(@NotNull Project project) {
            this.project = project;
        }

        @Override
        public long getModificationCount() {
            long hash = getTranslationRoot(this.project)
                .stream()
                .mapToLong(File::lastModified)
                .sum();

            if (hash != this.last) {
                this.last = hash;
                this.incModificationCount();
            }

            return super.getModificationCount();
        }
    }
}
