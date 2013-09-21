package fr.adrienbrault.idea.symfony2plugin.config;


import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceMap;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class PhpClassServiceGotoDeclarationHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int offset, Editor editor) {
        if(!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContext() instanceof PhpClass)) {
            return new PsiElement[0];
        }

        PhpClass phpClass = (PhpClass) psiElement.getContext();
        String phpClassName = phpClass.getPresentableFQN();

        ServiceMap serviceMap = ServiceXmlParserFactory.getInstance(psiElement.getProject(), XmlServiceParser.class).getServiceMap();
        String serviceName = serviceMap.resolveClassName(phpClassName);

        if(serviceName == null) {
            return new PsiElement[0];
        }

        ArrayList<PsiElement> serviceTargets = getPossibleServiceTarget(psiElement.getProject(), serviceName);
        return serviceTargets.toArray(new PsiElement[serviceTargets.size()]);

    }

    @Nullable
    @Override
    public String getActionText(DataContext context) {
        return null;
    }



    public static ArrayList<PsiElement> getPossibleServiceTarget(Project project, String serverName) {

        ArrayList<PsiElement> items = new ArrayList<PsiElement>();

        FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();
        Collection<VirtualFile> twigVirtualFiles = fileBasedIndex.getContainingFiles(FileTypeIndex.NAME, YAMLFileType.YML, GlobalSearchScope.projectScope(project));

        for (VirtualFile twigVirtualFile : twigVirtualFiles) {
            YAMLFile twigFile = (YAMLFile) PsiManager.getInstance(project).findFile(twigVirtualFile);
            PsiElement servicePsiElement = YamlHelper.getLocalServiceName(twigFile, serverName);
            if(servicePsiElement != null) {
                items.add(servicePsiElement);
            }
        }

        return items;
    }

}
