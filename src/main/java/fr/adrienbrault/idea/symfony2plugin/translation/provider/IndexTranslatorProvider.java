package fr.adrienbrault.idea.symfony2plugin.translation.provider;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.PhpIndex;
import fr.adrienbrault.idea.symfony2plugin.extension.TranslatorProviderDict;
import fr.adrienbrault.idea.symfony2plugin.stubs.SymfonyProcessors;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TranslationStubIndex;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationIndex;
import fr.adrienbrault.idea.symfony2plugin.extension.TranslatorProvider;
import fr.adrienbrault.idea.symfony2plugin.translation.collector.YamlTranslationCollector;
import fr.adrienbrault.idea.symfony2plugin.translation.collector.YamlTranslationVisitor;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLFile;

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

        // collect on index
        final YamlTranslationCollector translationCollector = (keyName, yamlKeyValue) -> {
            if (keyName.equals(translationKey)) {

                // multiline "line values" are not resolve properly on psiElements use key as fallback target
                PsiElement valuePsiElement = yamlKeyValue.getValue();
                psiFoundElements.add(valuePsiElement != null ? valuePsiElement : yamlKeyValue);

                return false;
            }

            return true;
        };

        FileBasedIndex.getInstance().getFilesWithKey(TranslationStubIndex.KEY, new HashSet<>(Collections.singletonList(domain)), virtualFile -> {
            // prevent duplicate targets and dont walk same file twice
            // if(virtualFilesFound.contains(virtualFile)) {
            //    return true;
            // }

            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if(psiFile == null) {
                return true;
            }

            if(psiFile instanceof YAMLFile) {
                YamlTranslationVisitor.collectFileTranslations((YAMLFile) psiFile, translationCollector);
            } else if(TranslationUtil.isSupportedXlfFile(psiFile)) {
                // fine: xlf registered as XML file. try to find source value
                psiFoundElements.addAll(TranslationUtil.getTargetForXlfAsXmlFile((XmlFile) psiFile, translationKey));
            } else if(("xlf".equalsIgnoreCase(virtualFile.getExtension()) || "xliff".equalsIgnoreCase(virtualFile.getExtension()))) {
                // xlf are plain text because not supported by jetbrains
                // for now we can only set file target
                psiFoundElements.addAll(FileBasedIndex.getInstance()
                    .getValues(TranslationStubIndex.KEY, domain, GlobalSearchScope.filesScope(project, Collections.singletonList(virtualFile))).stream()
                    .filter(string -> string.contains(translationKey)).map(string -> psiFile)
                    .collect(Collectors.toList())
                );
            }

            return true;
        }, GlobalSearchScope.allScope(project));

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
