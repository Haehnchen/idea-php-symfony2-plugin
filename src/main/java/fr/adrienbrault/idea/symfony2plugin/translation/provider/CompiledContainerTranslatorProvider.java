package fr.adrienbrault.idea.symfony2plugin.translation.provider;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.extension.TranslatorProvider;
import fr.adrienbrault.idea.symfony2plugin.extension.TranslatorProviderDict;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Extract domain information based on the the compiled debug container inisde the cache folder
 *
 * * var/cache/...container.xml
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class CompiledContainerTranslatorProvider implements TranslatorProvider {
    @Override
    public boolean hasTranslationKey(@NotNull Project project, @NotNull String keyName, @NotNull String domainName) {
        // we only have the file; content should already be inside our much faster index
        return false;
    }

    @Override
    public boolean hasDomain(@NotNull Project project, @NotNull String domainName) {
        return TranslationUtil.hasDomainInsideCompiledContainer(project, domainName);
    }

    @NotNull
    @Override
    public Collection<TranslatorProviderDict.TranslationDomain> getTranslationDomains(@NotNull Project project) {
        return TranslationUtil.getDomainsFromContainer(project).stream()
            .map(TranslatorProviderDict.TranslationDomain::new)
            .collect(Collectors.toList());
    }

    @NotNull
    @Override
    public Collection<PsiElement> getTranslationTargets(@NotNull Project project, @NotNull String translationKey, @NotNull String domain) {
        return TranslationUtil.getTranslationKeyFromCompiledContainerDomain(project, domain, translationKey);
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getDomainPsiFiles(Project project, @NotNull String domainName) {
        return TranslationUtil.getDomainFilesFromCompiledContainer(project, domainName);
    }

    @NotNull
    @Override
    public Collection<TranslatorProviderDict.TranslationKey> getTranslationsForDomain(@NotNull Project project, @NotNull String domainName) {
        // we only have the file; content should already be inside our much faster index
        return Collections.emptyList();
    }
}
