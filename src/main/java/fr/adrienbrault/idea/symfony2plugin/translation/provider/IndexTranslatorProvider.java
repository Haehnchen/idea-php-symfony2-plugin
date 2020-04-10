package fr.adrienbrault.idea.symfony2plugin.translation.provider;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.PhpIndex;
import fr.adrienbrault.idea.symfony2plugin.extension.TranslatorProvider;
import fr.adrienbrault.idea.symfony2plugin.extension.TranslatorProviderDict;
import fr.adrienbrault.idea.symfony2plugin.stubs.SymfonyProcessors;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TranslationStubIndex;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Use the internal index (yaml, xlf, ...) to provide translations
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class IndexTranslatorProvider implements TranslatorProvider {
    @Override
    public boolean hasTranslationKey(@NotNull Project project, @NotNull String keyName, @NotNull String domainName) {
        for(Set<String> keys: FileBasedIndex.getInstance().getValues(TranslationStubIndex.KEY, domainName, GlobalSearchScope.allScope(project))){
            if(keys.contains(keyName)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean hasDomain(@NotNull Project project, @NotNull String domainName) {
        return FileBasedIndex.getInstance().getValues(
            TranslationStubIndex.KEY,
            domainName,
            GlobalSearchScope.allScope(project)
        ).size() > 0;
    }

    @NotNull
    @Override
    public Collection<TranslatorProviderDict.TranslationDomain> getTranslationDomains(@NotNull Project project) {
        return SymfonyProcessors
            .createResult(project, TranslationStubIndex.KEY)
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
        return FileBasedIndex.getInstance()
            .getValues(TranslationStubIndex.KEY, domainName, GlobalSearchScope.allScope(project))
            .stream()
            .flatMap(Collection::stream)
            .map(key -> new TranslatorProviderDict.TranslationKey(key, true))
            .collect(Collectors.toSet());
    }
}
