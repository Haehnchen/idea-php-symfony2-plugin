package fr.adrienbrault.idea.symfony2plugin.util.service;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.dic.ServicesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceMap;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class ServiceIndexUtil {

    public static VirtualFile[] getFindServiceDefinitionFiles(Project project, String... serviceName) {

        final List<VirtualFile> virtualFiles = new ArrayList<VirtualFile> ();

        FileBasedIndexImpl.getInstance().getFilesWithKey(ServicesStubIndex.KEY, new HashSet<String>(Arrays.asList(serviceName)), new Processor<VirtualFile>() {
            @Override
            public boolean process(VirtualFile virtualFile) {
                virtualFiles.add(virtualFile);
                return true;
            }
        }, PhpIndex.getInstance(project).getSearchScope());

        return virtualFiles.toArray(new VirtualFile[virtualFiles.size()]);

    }

    public static PsiElement[] getPossibleServiceTargets(Project project, String serviceName) {

        ArrayList<PsiElement> items = new ArrayList<PsiElement>();

        VirtualFile[] twigVirtualFiles = ServiceIndexUtil.getFindServiceDefinitionFiles(project, serviceName);

        for (VirtualFile twigVirtualFile : twigVirtualFiles) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(twigVirtualFile);

            if(psiFile instanceof YAMLFile) {
                PsiElement servicePsiElement = YamlHelper.getLocalServiceName(psiFile, serviceName);
                if(servicePsiElement != null) {
                    items.add(servicePsiElement);
                }
            }

            if(psiFile instanceof XmlFile) {
                PsiElement servicePsiElement = XmlHelper.getLocalServiceName(psiFile, serviceName);
                if(servicePsiElement != null) {
                    items.add(servicePsiElement);
                }
            }

        }

        return items.toArray(new PsiElement[items.size()]);
    }

    public static PsiElement[] getPossibleServiceTargets(PhpClass phpClass) {

        String phpClassName = phpClass.getPresentableFQN();

        ServiceMap serviceMap = ServiceXmlParserFactory.getInstance(phpClass.getProject(), XmlServiceParser.class).getServiceMap();
        String serviceName = serviceMap.resolveClassName(phpClassName);

        if(serviceName == null) {
            return new PsiElement[0];
        }

        return getPossibleServiceTargets(phpClass.getProject(), serviceName);
    }


}
