package fr.adrienbrault.idea.symfony2plugin.translation.provider;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.extension.TranslatorProvider;
import fr.adrienbrault.idea.symfony2plugin.extension.TranslatorProviderDict;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationIndex;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Extract translations out of compiled php file inside Symfony cache folder
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class CompiledTranslatorProvider implements TranslatorProvider {
    @Override
    public boolean hasTranslationKey(@NotNull Project project, @NotNull String keyName, @NotNull String domainName) {
        Set<String> domainMap = TranslationIndex.getTranslationMap(project).getDomainMap(domainName);
        return domainMap != null && domainMap.contains(keyName);
    }

    @Override
    public boolean hasDomain(@NotNull Project project, @NotNull String domainName) {
        return TranslationIndex.getTranslationMap(project).getDomainList().contains(domainName);
    }

    @NotNull
    @Override
    public Collection<TranslatorProviderDict.TranslationDomain> getTranslationDomains(@NotNull Project project) {
        return TranslationIndex.getTranslationMap(project)
            .getDomainList()
            .stream()
            .map(TranslatorProviderDict.TranslationDomain::new)
            .collect(Collectors.toList());
    }

    @NotNull
    @Override
    public Collection<PsiElement> getTranslationTargets(@NotNull Project project, @NotNull String translationKey, @NotNull String domain) {
        // targeting cache folder is not helpful for user
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getDomainPsiFiles(Project project, @NotNull String domainName) {
        // targeting cache folder is not helpful for user
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<TranslatorProviderDict.TranslationKey> getTranslationsForDomain(@NotNull Project project, @NotNull String domainName) {
        Set<String> domainMap = TranslationIndex.getTranslationMap(project).getDomainMap(domainName);
        if (domainMap == null) {
            return Collections.emptyList();
        }

        return domainMap.stream().map(TranslatorProviderDict.TranslationKey::new).collect(Collectors.toList());
    }
}
