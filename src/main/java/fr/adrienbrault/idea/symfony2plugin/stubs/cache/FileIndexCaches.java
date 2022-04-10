package fr.adrienbrault.idea.symfony2plugin.stubs.cache;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.ID;
import fr.adrienbrault.idea.symfony2plugin.stubs.SymfonyProcessors;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * All FileBasedIndex are slow and cross project data, we need them every often
 * Cache values as long nothing globally change in our project.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FileIndexCaches {

    /**
     * @param dataHolderKey Main data to cache
     * @param dataHolderNames Cache extracted name Set
     */
    static public synchronized <T> Map<String, List<T>> getSetDataCache(@NotNull final Project project, @NotNull Key<CachedValue<Map<String, List<T>>>> dataHolderKey, final @NotNull Key<CachedValue<Set<String>>> dataHolderNames, @NotNull final ID<String, T> ID, @NotNull final GlobalSearchScope scope) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            dataHolderKey,
            () -> {
                Map<String, List<T>> items = new HashMap<>();

                final FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();

                getIndexKeysCache(project, dataHolderNames, ID).forEach(service ->
                    items.put(service, fileBasedIndex.getValues(ID, service, scope))
                );

                return CachedValueProvider.Result.create(items, getModificationTrackerForIndexId(project, ID));
            },
            false
        );
    }

    /**
     * @param dataHolderKey Main data to cache
     * @param dataHolderNames Cache extracted name Set
     */
    static public synchronized Map<String, List<String>> getStringDataCache(@NotNull final Project project, @NotNull Key<CachedValue<Map<String, List<String>>>> dataHolderKey, final @NotNull Key<CachedValue<Set<String>>> dataHolderNames, @NotNull final ID<String, String> ID, @NotNull final GlobalSearchScope scope) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            dataHolderKey,
            () -> {
                Map<String, List<String>> strings = new HashMap<>();

                final FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();
                getIndexKeysCache(project, dataHolderNames, ID).forEach(parameterName -> {
                    // just for secure
                    if(parameterName == null) {
                        return;
                    }

                    strings.put(parameterName, fileBasedIndex.getValues(ID, parameterName, scope));
                });

                return CachedValueProvider.Result.create(strings, getModificationTrackerForIndexId(project, ID));
            },
            false
        );
    }

    /**
     * There several methods that just need to check for names, as they also needed for value extraction, so cache them also
     */
    static public synchronized Set<String> getIndexKeysCache(@NotNull final Project project, @NotNull Key<CachedValue<Set<String>>> dataHolderKey, @NotNull final ID<String, ?> id) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            dataHolderKey,
            () -> CachedValueProvider.Result.create(SymfonyProcessors.createResult(project, id), getModificationTrackerForIndexId(project, id)),
            false
        );
    }

    @NotNull
    public static ModificationTracker getModificationTrackerForIndexId(@NotNull Project project, @NotNull final ID<?, ?> id) {
        return () -> FileBasedIndex.getInstance().getIndexModificationStamp(id, project);
    }
}
