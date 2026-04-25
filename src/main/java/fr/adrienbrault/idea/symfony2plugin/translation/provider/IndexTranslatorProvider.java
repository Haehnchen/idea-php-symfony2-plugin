package fr.adrienbrault.idea.symfony2plugin.translation.provider;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.PhpIndex;
import fr.adrienbrault.idea.symfony2plugin.extension.TranslatorProvider;
import fr.adrienbrault.idea.symfony2plugin.extension.TranslatorProviderDict;
import fr.adrienbrault.idea.symfony2plugin.stubs.SymfonyProcessors;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TranslationStubIndex;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Use the internal index (yaml, xlf, ...) to provide translations
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class IndexTranslatorProvider implements TranslatorProvider {
    private static final Key<CachedValue<Set<String>>> TRANSLATION_DOMAINS =
        Key.create("SYMFONY_TRANSLATION_DOMAINS");

    private static final Key<CachedValue<Map<String, Set<String>>>> TRANSLATION_KEYS_BY_DOMAIN =
        Key.create("SYMFONY_TRANSLATION_KEYS_BY_DOMAIN");

    @Override
    public boolean hasTranslationKey(@NotNull Project project, @NotNull String keyName, @NotNull String domainName) {
        return hasIndexedTranslationKey(project, keyName, domainName);
    }

    @Override
    public boolean hasDomain(@NotNull Project project, @NotNull String domainName) {
        return hasIndexedDomain(project, domainName);
    }

    @NotNull
    @Override
    public Collection<TranslatorProviderDict.TranslationDomain> getTranslationDomains(@NotNull Project project) {
        return getIndexedDomains(project)
            .stream()
            .map(s -> new TranslatorProviderDict.TranslationDomain(s, true))
            .collect(Collectors.toList());
    }

    @NotNull
    @Override
    public Collection<PsiElement> getTranslationTargets(@NotNull Project project, @NotNull String translationKey, @NotNull String domain) {
        Collection<PsiElement> psiFoundElements = new ArrayList<>();

        Collection<VirtualFile> files = new HashSet<>();
        FileBasedIndex.getInstance().getFilesWithKey(TranslationStubIndex.KEY, new HashSet<>(Collections.singletonList(domain)), virtualFile -> {
            files.add(virtualFile);
            return true;
        }, GlobalSearchScope.allScope(project));

        for (PsiFile psiFile : PsiElementUtils.convertVirtualFilesToPsiFiles(project, files)) {
            psiFoundElements.addAll(TranslationUtil.getTranslationKeyTargetInsideFile(psiFile, domain, translationKey));
        }

        return psiFoundElements;
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getDomainPsiFiles(Project project, @NotNull String domainName) {
        Collection<VirtualFile> targets = new HashSet<>();

        FileBasedIndex.getInstance().getFilesWithKey(TranslationStubIndex.KEY, new HashSet<>(Collections.singletonList(domainName)), virtualFile -> {
            targets.add(virtualFile);
            return true;
        }, PhpIndex.getInstance(project).getSearchScope());

        return targets;
    }

    @NotNull
    @Override
    public Collection<TranslatorProviderDict.TranslationKey> getTranslationsForDomain(@NotNull Project project, @NotNull String domainName) {
        return getIndexedTranslationKeys(project, domainName)
            .stream()
            .map(key -> new TranslatorProviderDict.TranslationKey(key, true))
            .collect(Collectors.toSet());
    }

    @NotNull
    private static Map<String, Set<String>> getTranslationKeysByDomain(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            TRANSLATION_KEYS_BY_DOMAIN,
            () -> CachedValueProvider.Result.create(
                new ConcurrentHashMap<>(),
                FileIndexCaches.getModificationTrackerForIndexId(project, TranslationStubIndex.KEY)
            ),
            false
        );
    }

    public static boolean hasIndexedTranslationKey(@NotNull Project project, @NotNull String keyName, @NotNull String domainName) {
        return getIndexedTranslationKeys(project, domainName).contains(keyName);
    }

    public static boolean hasIndexedDomain(@NotNull Project project, @NotNull String domainName) {
        return getIndexedDomains(project).contains(domainName);
    }

    @NotNull
    private static Set<String> getIndexedDomains(@NotNull Project project) {
        return FileIndexCaches.getIndexKeysCache(project, TRANSLATION_DOMAINS, TranslationStubIndex.KEY);
    }

    @NotNull
    private static Set<String> getIndexedTranslationKeys(@NotNull Project project, @NotNull String domainName) {
        if (!hasIndexedDomain(project, domainName)) {
            return Collections.emptySet();
        }

        return getTranslationKeysByDomain(project).computeIfAbsent(domainName, domain -> {
            FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();
            GlobalSearchScope scope = GlobalSearchScope.allScope(project);

            return fileBasedIndex.getValues(TranslationStubIndex.KEY, domain, scope)
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        });
    }
}
