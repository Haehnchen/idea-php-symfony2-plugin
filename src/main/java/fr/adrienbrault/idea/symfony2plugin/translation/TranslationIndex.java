package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ModificationTracker;
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
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import fr.adrienbrault.idea.symfony2plugin.util.TimeSecondModificationTracker;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationIndex {
    private static final Key<CachedValue<Collection<File>>> SYMFONY_TRANSLATION_COMPILED_TIMED_WATCHER = new Key<>("SYMFONY_TRANSLATION_COMPILED_TIMED_WATCHER");
    private static final Key<CachedValue<Collection<File>>> SYMFONY_TRANSLATION_COMPILED = new Key<>("SYMFONY_TRANSLATION_COMPILED");

    private TranslationIndex() {
    }

    private static final Key<CachedValue<TranslationStringMap>> CACHE = new Key<>("FOO");

    synchronized static public TranslationStringMap getTranslationMap(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            CACHE,
            () -> {
                Collection<File> translationDirectories = getTranslationRoot(project);

                TranslationStringMap translationStringMap;
                if (translationDirectories.size() > 0) {
                    translationStringMap = TranslationStringMap.create(project, translationDirectories);
                } else {
                    translationStringMap = TranslationStringMap.createEmpty();
                }

                Symfony2ProjectComponent.getLogger().info("translations changed: " + StringUtils.join(translationDirectories.stream().map(File::toString).collect(Collectors.toSet()), ","));

                return CachedValueProvider.Result.create(translationStringMap, new MyModificationTracker(project));
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

    @NotNull
    private static Collection<File> getTranslationRootInner(@NotNull Project project) {
        Collection<File> files = new HashSet<>();

        String translationPath = Settings.getInstance(project).pathToTranslation;
        if (StringUtils.isNotBlank(translationPath)) {
            if (!FileUtil.isAbsolute(translationPath)) {
                translationPath = project.getBasePath() + "/" + translationPath;
            }

            File file = new File(translationPath);
            if(file.exists() && file.isDirectory()) {
                files.add(file);
            }
        }

        VirtualFile baseDir = ProjectUtil.getProjectDir(project);

        for (String containerFile : ServiceContainerUtil.getContainerFiles(project)) {
            // resolve the file
            VirtualFile containerVirtualFile = VfsUtil.findRelativeFile(containerFile, baseDir);
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
                files.add(VfsUtilCore.virtualToIoFile(translations));
            }
        }

        return files;
    }

    private static class MyModificationTracker implements ModificationTracker {
        @NotNull
        private final Project project;

        public MyModificationTracker(@NotNull Project project) {
            this.project = project;
        }

        @Override
        public long getModificationCount() {
            return getTranslationRoot(this.project).stream().mapToLong(File::lastModified).sum();
        }
    }
}
