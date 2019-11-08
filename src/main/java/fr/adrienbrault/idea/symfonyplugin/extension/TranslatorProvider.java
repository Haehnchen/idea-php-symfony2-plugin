package fr.adrienbrault.idea.symfonyplugin.extension;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Provide extension implementation for new translator source
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface TranslatorProvider {
    boolean hasTranslationKey(@NotNull Project project, @NotNull String keyName, @NotNull String domainName);
    boolean hasDomain(@NotNull Project project, @NotNull String domainName);

    @NotNull
    Collection<TranslatorProviderDict.TranslationDomain> getTranslationDomains(@NotNull Project project);

    @NotNull
    Collection<PsiElement> getTranslationTargets(@NotNull Project project, @NotNull String translationKey, @NotNull String domain);

    @NotNull
    Collection<VirtualFile> getDomainPsiFiles(final Project project, @NotNull String domainName);

    @NotNull
    Collection<TranslatorProviderDict.TranslationKey> getTranslationsForDomain(@NotNull Project project, @NotNull String domainName);
}
