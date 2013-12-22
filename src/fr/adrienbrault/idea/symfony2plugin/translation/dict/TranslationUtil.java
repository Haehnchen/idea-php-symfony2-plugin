package fr.adrienbrault.idea.symfony2plugin.translation.dict;


import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationIndex;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.DomainMappings;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringMap;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlKeyFinder;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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


    public static PsiElement[] getTranslationPsiElements(Project project, String translationKey, String domain) {

        // search for available domain files
        PsiElement[] psiTranslationFiles = getDomainFilePsiElements(project, domain);

        ArrayList<PsiElement> psiFoundElements = new ArrayList<PsiElement>();
        for(PsiElement psiTranslationFile : psiTranslationFiles) {
            PsiFile psiFile = PsiElementUtils.virtualFileToPsiFile(project, psiTranslationFile.getContainingFile().getVirtualFile());
            if(psiFile instanceof YAMLFile) {
                PsiElement yamlDocu = PsiTreeUtil.findChildOfType(psiFile, YAMLDocument.class);
                if(yamlDocu != null) {
                    YAMLKeyValue goToPsi = YamlKeyFinder.findKeyValueElement(yamlDocu, translationKey);
                    if(goToPsi != null) {
                        // multiline are line values are not resolve properly on psiElements use key as fallback target
                        PsiElement valuePsiElement = goToPsi.getValue();
                        psiFoundElements.add(valuePsiElement != null ? valuePsiElement : goToPsi);
                    }
                }

            }

        }

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

}
