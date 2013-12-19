package fr.adrienbrault.idea.symfony2plugin.stubs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceMap;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ServicesDefinitionStubIndex;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.*;

public class ServiceIndexUtil {

    public static VirtualFile[] getFindServiceDefinitionFiles(Project project, String... serviceName) {

        final List<VirtualFile> virtualFiles = new ArrayList<VirtualFile> ();

        FileBasedIndexImpl.getInstance().getFilesWithKey(ServicesDefinitionStubIndex.KEY, new HashSet<String>(Arrays.asList(serviceName)), new Processor<VirtualFile>() {
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
            Set<String> serviceNames = ServiceIndexUtil.getServiceNamesFromClassName(phpClass.getProject(), phpClass.getPresentableFQN());
            if(serviceNames.size() > 0) {
                serviceName = serviceNames.iterator().next();
            }
        }

        if(serviceName == null) {
            return new PsiElement[0];
        }

        return getPossibleServiceTargets(phpClass.getProject(), serviceName);
    }

    public static Collection<String> getAllServiceNames(Project project) {
        return FileBasedIndexImpl.getInstance().getAllKeys(ServicesDefinitionStubIndex.KEY, project);
    }

    @Nullable
    public static String getServiceClassOnIndex(Project project, String serviceName) {

        if(serviceName == null || StringUtils.isBlank(serviceName)) {
            return  null;
        }

        for(Set<String> rawServiceNames: FileBasedIndexImpl.getInstance().getValues(ServicesDefinitionStubIndex.KEY, serviceName, GlobalSearchScope.projectScope(project))) {
            if(rawServiceNames.size() > 0) {
                // first Set element is class name
                String first = rawServiceNames.iterator().next();
                if(first != null) {
                    return ContainerCollectionResolver.resolveParameterClass(project, first);
                }

            }
        }

        return null;
    }

    public static Map<String, String> getIndexedServiceMap(Project project) {

        Map<String, String> serviceMap = new HashMap<String, String>();

        for(String serviceName: getAllServiceNames(project)) {
            serviceMap.put(serviceName, getServiceClassOnIndex(project, serviceName));
        }

        return serviceMap;
    }

    public static Set<String> getServiceNamesFromClassName(Project project, String className) {

        Set<String> serviceNames = new HashSet<String>();

        // normalize class name; prepend "\"
        if(!className.startsWith("\\")) {
            className = "\\" + className;
        }

        for(String serviceName: FileBasedIndexImpl.getInstance().getAllKeys(ServicesDefinitionStubIndex.KEY, project)) {
            for(Set<String> test : FileBasedIndexImpl.getInstance().getValues(ServicesDefinitionStubIndex.KEY, serviceName, PhpIndex.getInstance(project).getSearchScope())) {
                if(test.size() > 0) {
                    String indexedClassName = ContainerCollectionResolver.resolveParameterClass(project, test.iterator().next());
                    if(indexedClassName != null) {

                        // also normalize user input string inside container
                        if(!indexedClassName.startsWith("\\")) {
                            indexedClassName = "\\" + indexedClassName;
                        }

                        if(indexedClassName.equals(className)) {
                            serviceNames.add(serviceName);
                        }
                    }

                }
            }
        }

        return serviceNames;
    }

    public static void attachIndexServiceResolveResults(Project project, String serviceName, List<ResolveResult> resolveResults) {

        PsiElement[] indexedPsiElements = ServiceIndexUtil.getPossibleServiceTargets(project, serviceName);
        if(indexedPsiElements.length == 0) {
            return;
        }

        for(PsiElement indexedPsiElement: indexedPsiElements) {
           resolveResults.add(new PsiElementResolveResult(indexedPsiElement));
        }

    }


}
