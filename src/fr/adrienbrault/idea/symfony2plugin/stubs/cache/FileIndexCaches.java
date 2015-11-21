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
import org.jetbrains.annotations.Nullable;

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
            cache = CachedValuesManager.getManager(project).createCachedValue(new CachedValueProvider<Map<String, List<T>>>() {
                @Nullable
                @Override
                public Result<Map<String, List<T>>> compute() {

                    Map<String, List<T>> items = new HashMap<String, List<T>>();

                    FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();
                    for (String serviceName : getIndexKeysCache(project, dataHolderNames, ID)) {
                        items.put(serviceName, fileBasedIndex.getValues(ID, serviceName, scope));
                    }

                    return Result.create(items, PsiModificationTracker.MODIFICATION_COUNT);
                }
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
            cache = CachedValuesManager.getManager(project).createCachedValue(new CachedValueProvider<Map<String, List<String>>>() {
                @Nullable
                @Override
                public Result<Map<String, List<String>>> compute() {

                    Map<String, List<String>> strings = new HashMap<String, List<String>>();

                    FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();
                    for(String parameterName: getIndexKeysCache(project, dataHolderNames, ID)) {

                        // just for secure
                        if(parameterName == null) {
                            continue;
                        }

                        strings.put(parameterName, fileBasedIndex.getValues(ID, parameterName, scope));
                    }

                    return Result.create(strings, PsiModificationTracker.MODIFICATION_COUNT);
                }
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
            cache = CachedValuesManager.getManager(project).createCachedValue(new CachedValueProvider<Set<String>>() {
                @Nullable
                @Override
                public Result<Set<String>> compute() {
                    SymfonyProcessors.CollectProjectUniqueKeys projectUniqueKeys = new SymfonyProcessors.CollectProjectUniqueKeys(project, ID);
                    FileBasedIndex.getInstance().processAllKeys(ID, projectUniqueKeys, project);
                    return Result.create(projectUniqueKeys.getResult(), PsiModificationTracker.MODIFICATION_COUNT);
                }
            }, false);

            project.putUserData(dataHolderKey, cache);
        }

        return cache.getValue();
    }

}
