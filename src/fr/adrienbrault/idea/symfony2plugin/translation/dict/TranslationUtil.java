package fr.adrienbrault.idea.symfony2plugin.translation.dict;


import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.DomainMappings;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;

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

}
