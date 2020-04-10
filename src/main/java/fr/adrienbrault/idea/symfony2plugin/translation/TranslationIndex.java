package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
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
import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationPsiParser;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringMap;
import fr.adrienbrault.idea.symfony2plugin.util.TimeSecondModificationTracker;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationIndex {
    private static final Key<CachedValue<Collection<File>>> SYMFONY_TRANSLATION_COMPILED_TIMED_WATCHER = new Key<>("SYMFONY_TRANSLATION_COMPILED_TIMED_WATCHER");
    private static final Key<CachedValue<Collection<File>>> SYMFONY_TRANSLATION_COMPILED = new Key<>("SYMFONY_TRANSLATION_COMPILED");

    private static Map<Project, TranslationIndex> instance = new HashMap<>();

    private Project project;

    @Nullable
    private TranslationStringMap translationStringMap;
    private long translationStringMapModified;

    public static TranslationIndex getInstance(@NotNull Project project){
        TranslationIndex projectInstance = instance.get(project);
        if(projectInstance != null) {
          return projectInstance;
        }

        projectInstance = new TranslationIndex(project);
        instance.put(project, projectInstance);

        return projectInstance;
    }

    private TranslationIndex(@NotNull Project project) {
        this.project = project;
    }

    synchronized public TranslationStringMap getTranslationMap() {
        if(this.translationStringMap != null && this.isCacheValid()) {
            return this.translationStringMap;
        }

        Collection<File> translationDirectories = this.getTranslationRoot();
        if(translationDirectories.size() == 0) {
            return new TranslationStringMap();
        }

        Symfony2ProjectComponent.getLogger().info("translations changed: " + StringUtils.join(translationDirectories.stream().map(File::toString).collect(Collectors.toSet()), ","));

        this.translationStringMapModified = translationDirectories.stream().mapToLong(File::lastModified).sum();
        return this.translationStringMap = new TranslationPsiParser(project, translationDirectories).parsePathMatcher();
    }

    private boolean isCacheValid() {
        // symfony2 recreates translation file on change, so folder modtime is caching indicator
        Collection<File> translationDirectories = this.getTranslationRoot();
        if(translationDirectories.size() == 0) {
            return false;
        }

        return translationDirectories.stream().mapToLong(File::lastModified).sum() == translationStringMapModified;
    }

    @NotNull
    private Collection<File> getTranslationRoot() {
        return CachedValuesManager.getManager(project)
            .getCachedValue(
                project,
                SYMFONY_TRANSLATION_COMPILED,
                () -> CachedValueProvider.Result.create(getTranslationRootInnerTime(), PsiModificationTracker.MODIFICATION_COUNT),
                false
            );
    }

    @NotNull
    private Collection<File> getTranslationRootInnerTime() {
        return CachedValuesManager.getManager(project).getCachedValue(project, SYMFONY_TRANSLATION_COMPILED_TIMED_WATCHER, () -> CachedValueProvider.Result.create(getTranslationRootInner(), TimeSecondModificationTracker.TIMED_MODIFICATION_TRACKER_60), false);
    }

    @NotNull
    private Collection<File> getTranslationRootInner() {
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

        VirtualFile baseDir = project.getBaseDir();

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
}
