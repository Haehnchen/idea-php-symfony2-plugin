package fr.adrienbrault.idea.symfony2plugin.stubs.cache;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
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

        CachedValue<Map<String, List<T>>> cache = project.getUserData(dataHolderKey);

        if(cache == null) {
            cache = CachedValuesManager.getManager(project).createCachedValue(() -> {
                Map<String, List<T>> items = new HashMap<>();

                final FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();

                getIndexKeysCache(project, dataHolderNames, ID).stream().forEach(service -> {
                    items.put(service, fileBasedIndex.getValues(ID, service, scope));
                });

                return CachedValueProvider.Result.create(items, PsiModificationTracker.MODIFICATION_COUNT);
            }, false);

            project.putUserData(dataHolderKey, cache);
        }

        return cache.getValue();
    }

    /**
     * @param dataHolderKey Main data to cache
     * @param dataHolderNames Cache extracted name Set
     */
    static public synchronized Map<String, List<String>> getStringDataCache(@NotNull final Project project, @NotNull Key<CachedValue<Map<String, List<String>>>> dataHolderKey, final @NotNull Key<CachedValue<Set<String>>> dataHolderNames, @NotNull final ID<String, String> ID, @NotNull final GlobalSearchScope scope) {

        CachedValue<Map<String, List<String>>> cache = project.getUserData(dataHolderKey);
        if(cache == null) {
            cache = CachedValuesManager.getManager(project).createCachedValue(() -> {

                Map<String, List<String>> strings = new HashMap<>();

                final FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();
                getIndexKeysCache(project, dataHolderNames, ID).stream().forEach(parameterName -> {
                    // just for secure
                    if(parameterName == null) {
                        return;
                    }

                    strings.put(parameterName, fileBasedIndex.getValues(ID, parameterName, scope));
                });

                return CachedValueProvider.Result.create(strings, PsiModificationTracker.MODIFICATION_COUNT);
            }, false);

            project.putUserData(dataHolderKey, cache);
        }

        return cache.getValue();
    }

    /**
     * There several methods that just need to check for names, as they also needed for value extraction, so cache them also
     */
    static public synchronized Set<String> getIndexKeysCache(@NotNull final Project project, @NotNull Key<CachedValue<Set<String>>> dataHolderKey, @NotNull final ID<String, ?> ID) {

        CachedValue<Set<String>> cache = project.getUserData(dataHolderKey);

        if(cache == null) {
            cache = CachedValuesManager.getManager(project).createCachedValue(() -> {
                SymfonyProcessors.CollectProjectUniqueKeys projectUniqueKeys = new SymfonyProcessors.CollectProjectUniqueKeys(project, ID);
                FileBasedIndex.getInstance().processAllKeys(ID, projectUniqueKeys, project);
                return CachedValueProvider.Result.create(projectUniqueKeys.getResult(), PsiModificationTracker.MODIFICATION_COUNT);
            }, false);

            project.putUserData(dataHolderKey, cache);
        }

        return cache.getValue();
    }

}
