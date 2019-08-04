package fr.adrienbrault.idea.symfony2plugin.translation.provider;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.indexing.FileBasedIndex;
import fr.adrienbrault.idea.symfony2plugin.extension.TranslatorProviderDict;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TranslationStubIndex;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationIndex;
import fr.adrienbrault.idea.symfony2plugin.extension.TranslatorProvider;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Extract translations out of compiled php file inside Symfony cache folder
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class CompiledTranslatorProvider implements TranslatorProvider {
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
        return TranslationIndex.getInstance(project)
            .getTranslationMap()
            .getDomainList()
            .stream()
            .map(TranslatorProviderDict.TranslationDomain::new)
            .collect(Collectors.toList());
    }

    @NotNull
    @Override
    public Collection<PsiElement> getTranslationTargets(@NotNull Project project, @NotNull String translationKey, @NotNull String domain) {
        Collection<PsiElement> psiFoundElements = new ArrayList<>();

        // @TODO: completely remove this? support translation paths from service compiler
        // search for available domain files
        for(VirtualFile translationVirtualFile : TranslationUtil.getDomainFilePsiElements(project, domain)) {
            if(translationVirtualFile.getFileType() != YAMLFileType.YML) {
                continue;
            }

            PsiFile psiFile = PsiElementUtils.virtualFileToPsiFile(project, translationVirtualFile);
            if(psiFile instanceof YAMLFile) {
                PsiElement yamlDocu = PsiTreeUtil.findChildOfType(psiFile, YAMLDocument.class);
                if(yamlDocu != null) {
                    YAMLKeyValue goToPsi = YAMLUtil.getQualifiedKeyInFile((YAMLFile) psiFile, translationKey.split("\\."));
                    if(goToPsi != null) {
                        // multiline are line values are not resolve properly on psiElements use key as fallback target
                        PsiElement valuePsiElement = goToPsi.getValue();
                        psiFoundElements.add(valuePsiElement != null ? valuePsiElement : goToPsi);
                    }
                }
            }
        }

        return psiFoundElements;
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getDomainPsiFiles(Project project, @NotNull String domainName) {
        return Arrays.asList(TranslationUtil.getDomainFilePsiElements(project, domainName));
    }

    @NotNull
    @Override
    public Collection<TranslatorProviderDict.TranslationKey> getTranslationsForDomain(@NotNull Project project, @NotNull String domainName) {
        Set<String> domainMap = TranslationIndex.getInstance(project).getTranslationMap().getDomainMap(domainName);
        if (domainMap == null) {
            return Collections.emptyList();
        }

        return domainMap.stream().map(TranslatorProviderDict.TranslationKey::new).collect(Collectors.toList());
    }
}
