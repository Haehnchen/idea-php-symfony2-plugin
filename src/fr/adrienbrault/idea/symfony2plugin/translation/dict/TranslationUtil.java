package fr.adrienbrault.idea.symfony2plugin.translation.dict;


import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.jetbrains.php.PhpIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.YamlTranslationStubIndex;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationIndex;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslatorLookupElement;
import fr.adrienbrault.idea.symfony2plugin.translation.collector.YamlTranslationCollector;
import fr.adrienbrault.idea.symfony2plugin.translation.collector.YamlTranslationVistor;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.DomainMappings;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringMap;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlKeyFinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.*;

public class TranslationUtil {

    static public PsiElement[] getDomainFilePsiElements(Project project, String domainName) {

        DomainMappings domainMappings = ServiceXmlParserFactory.getInstance(project, DomainMappings.class);
        List<PsiElement> psiElements = new ArrayList<PsiElement>();

        for(DomainFileMap domain: domainMappings.getDomainFileMaps()) {
            if(domain.getDomain().equals(domainName)) {
                PsiFile psiFile = domain.getPsiFile(project);
                if(psiFile != null) {
                    psiElements.add(psiFile);
                }
            }
        }

        return psiElements.toArray(new PsiElement[psiElements.size()]);
    }


    public static PsiElement[] getTranslationPsiElements(final Project project, final String translationKey, String domain) {

        // search for available domain files
        PsiElement[] psiTranslationFiles = getDomainFilePsiElements(project, domain);

        final List<PsiElement> psiFoundElements = new ArrayList<PsiElement>();
        final List<VirtualFile> virtualFilesFound = new ArrayList<VirtualFile>();

        // @TODO: this is completely can remove, after stable index
        for(PsiElement psiTranslationFile : psiTranslationFiles) {
            VirtualFile virtualFile = psiTranslationFile.getContainingFile().getVirtualFile();
            PsiFile psiFile = PsiElementUtils.virtualFileToPsiFile(project, virtualFile);
            if(psiFile instanceof YAMLFile) {
                PsiElement yamlDocu = PsiTreeUtil.findChildOfType(psiFile, YAMLDocument.class);
                if(yamlDocu != null) {
                    YAMLKeyValue goToPsi = YamlKeyFinder.findKeyValueElement(yamlDocu, translationKey);
                    if(goToPsi != null) {
                        // multiline are line values are not resolve properly on psiElements use key as fallback target
                        PsiElement valuePsiElement = goToPsi.getValue();
                        psiFoundElements.add(valuePsiElement != null ? valuePsiElement : goToPsi);
                        virtualFilesFound.add(virtualFile);
                    }
                }

            }

        }

        // collect on index
        final YamlTranslationCollector translationCollector = new YamlTranslationCollector() {
            @Override
            public boolean collect(@NotNull String keyName, YAMLKeyValue yamlKeyValue) {
                if (keyName.equals(translationKey)) {

                    // multiline are line values are not resolve properly on psiElements use key as fallback target
                    PsiElement valuePsiElement = yamlKeyValue.getValue();
                    psiFoundElements.add(valuePsiElement != null ? valuePsiElement : yamlKeyValue);

                    return false;
                }

                return true;
            }
        };

        FileBasedIndexImpl.getInstance().getFilesWithKey(YamlTranslationStubIndex.KEY, new HashSet<String>(Arrays.asList(domain)), new Processor<VirtualFile>() {
            @Override
            public boolean process(VirtualFile virtualFile) {

                // prevent duplicate targets and dont walk same file twice
                if(virtualFilesFound.contains(virtualFile)) {
                    return true;
                }

                PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                if(psiFile instanceof YAMLFile) {
                    YamlTranslationVistor.collectFileTranslations((YAMLFile) psiFile, translationCollector);
                }

                return true;
            }
        }, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), YAMLFileType.YML));


        return psiFoundElements.toArray(new PsiElement[psiFoundElements.size()]);
    }

    public static boolean hasTranslationKey(Project project, String keyName, String domainName) {

        if(TranslationIndex.getInstance(project).getTranslationMap().getDomainList().contains(domainName)) {
            return false;
        }

        Set<String> domainMap = TranslationIndex.getInstance(project).getTranslationMap().getDomainMap(domainName);
        if(domainMap == null) {
            return false;
        }

        return domainMap.contains(keyName);
    }


    public static List<LookupElement> getTranslationLookupElementsOnDomain(Project project, String domainName) {

        Set<String> keySet = new HashSet<String>();
        List<String[]> test = FileBasedIndexImpl.getInstance().getValues(YamlTranslationStubIndex.KEY, domainName, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), YAMLFileType.YML));
        for(String keys[]: test ){
            keySet.addAll(Arrays.asList(keys));
        }

        List<LookupElement> lookupElements = new ArrayList<LookupElement>();

        TranslationStringMap map = TranslationIndex.getInstance(project).getTranslationMap();
        Collection<String> domainMap = map.getDomainMap(domainName);

        if(domainMap != null) {

            // php translation parser; are not weak and valid keys
            for(String stringId : domainMap) {
                lookupElements.add(new TranslatorLookupElement(stringId, domainName));
            }

            // attach weak translations keys on file index
            for(String stringId : keySet) {
                if(!domainMap.contains(stringId)) {
                    lookupElements.add(new TranslatorLookupElement(stringId, domainName, true));
                }
            }

            return lookupElements;
        }

        // fallback on index
        for(String stringId : keySet) {
            lookupElements.add(new TranslatorLookupElement(stringId, domainName, true));
        }

        return lookupElements;
    }

    public static List<LookupElement> getTranslationDomainLookupElements(Project project) {

        List<LookupElement> lookupElements = new ArrayList<LookupElement>();

        // domains on complied file
        TranslationStringMap map = TranslationIndex.getInstance(project).getTranslationMap();
        Set<String> domainList = map.getDomainList();
        for(String domainKey : domainList) {
            lookupElements.add(new TranslatorLookupElement(domainKey, domainKey));
        }

        // attach index domains as weak one
        for(String domainKey: FileBasedIndexImpl.getInstance().getAllKeys(YamlTranslationStubIndex.KEY, project)) {
            if(!domainList.contains(domainKey)) {
                lookupElements.add(new TranslatorLookupElement(domainKey, domainKey, true));
            }
        }

        return lookupElements;
    }

    public static List<PsiFile> getDomainPsiFiles(final Project project, String domainName) {

        final List<PsiFile> results = new ArrayList<PsiFile>();

        PsiElement[] psiElements = TranslationUtil.getDomainFilePsiElements(project, domainName);

        final List<PsiFile> uniqueFileList = new ArrayList<PsiFile>();

        /* for (PsiElement psiElement : psiElements) {
            if(psiElement instanceof PsiFile) {
                uniqueFileList.add((PsiFile) psiElement);
                results.add((PsiFile) psiElement);
            }
        } */

        FileBasedIndexImpl.getInstance().getFilesWithKey(YamlTranslationStubIndex.KEY, new HashSet<String>(Arrays.asList(domainName)), new Processor<VirtualFile>() {
            @Override
            public boolean process(VirtualFile virtualFile) {

                PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                if(psiFile != null && !uniqueFileList.contains(psiFile)) {
                    uniqueFileList.add(psiFile);
                    results.add(psiFile);
                }

                return true;
            }
        }, PhpIndex.getInstance(project).getSearchScope());

        return results;
    }

}
