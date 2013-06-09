package fr.adrienbrault.idea.symfony2plugin.translation.dict;


import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.DomainMappings;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlKeyFinder;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.ArrayList;
import java.util.List;

public class TranslationUtil {

    static public PsiElement[] getDomainFilePsiElements(Project project, String domainName) {

        ServiceXmlParserFactory xmlParser = ServiceXmlParserFactory.getInstance(project, DomainMappings.class);

        Object domains = xmlParser.parser();
        if(domains == null || !(domains instanceof ArrayList)) {
            return new PsiElement[0];
        }

        List<PsiElement> psiElements = new ArrayList<PsiElement>();

        for(DomainFileMap domain: (ArrayList<DomainFileMap>) domains) {
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
                    PsiElement goToPsi = YamlKeyFinder.findKeyValueElement(yamlDocu, translationKey);
                    if(goToPsi != null) {
                        psiFoundElements.add(goToPsi);
                    }
                }

            }

        }

        return psiFoundElements.toArray(new PsiElement[psiFoundElements.size()]);
    }

}
