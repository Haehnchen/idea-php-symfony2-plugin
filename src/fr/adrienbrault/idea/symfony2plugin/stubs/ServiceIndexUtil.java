package fr.adrienbrault.idea.symfony2plugin.stubs;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.ClassServiceDefinitionTargetLazyValue;
import fr.adrienbrault.idea.symfony2plugin.extension.ServiceCollector;
import fr.adrienbrault.idea.symfony2plugin.extension.ServiceDefinitionLocator;
import fr.adrienbrault.idea.symfony2plugin.extension.ServiceDefinitionLocatorParameter;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ServicesDefinitionStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.*;

public class ServiceIndexUtil {

    private static final ExtensionPointName<ServiceDefinitionLocator> EXTENSIONS = new ExtensionPointName<>(
        "fr.adrienbrault.idea.symfony2plugin.extension.ServiceDefinitionLocator"
    );

    private static VirtualFile[] findServiceDefinitionFiles(@NotNull Project project, @NotNull String serviceName) {

        final List<VirtualFile> virtualFiles = new ArrayList<>();

        FileBasedIndexImpl.getInstance().getFilesWithKey(ServicesDefinitionStubIndex.KEY, new HashSet<>(Collections.singletonList(serviceName.toLowerCase())), virtualFile -> {
            virtualFiles.add(virtualFile);
            return true;
        }, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), XmlFileType.INSTANCE, YAMLFileType.YML));

        return virtualFiles.toArray(new VirtualFile[virtualFiles.size()]);

    }

    public static List<PsiElement> findServiceDefinitions(@NotNull Project project, @NotNull String serviceName) {

        List<PsiElement> items = new ArrayList<PsiElement>();

        VirtualFile[] twigVirtualFiles = ServiceIndexUtil.findServiceDefinitionFiles(project, serviceName);

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

        // extension points
        ServiceDefinitionLocator[] extensions = EXTENSIONS.getExtensions();
        if(extensions.length > 0) {
            ServiceDefinitionLocatorParameter parameter = new ServiceDefinitionLocatorParameter(project, items);
            for (ServiceDefinitionLocator locator : extensions) {
                locator.locate(serviceName, parameter);
            }
        }

        return items;
    }

    public static List<PsiElement> findParameterDefinitions(@NotNull PsiFile psiFile, @NotNull String parameterName) {

        List<PsiElement> items = new ArrayList<PsiElement>();

        if(psiFile instanceof YAMLFile) {
            PsiElement servicePsiElement = YamlHelper.getLocalParameterMap(psiFile, parameterName);
            if(servicePsiElement != null) {
                items.add(servicePsiElement);
            }
        }

        if(psiFile instanceof XmlFile) {
            PsiElement localParameterName = XmlHelper.getLocalParameterName(psiFile, parameterName);
            if(localParameterName != null) {
                items.add(localParameterName);
            }
        }

        return items;
    }

    public static PsiElement[] findServiceDefinitions(@Nullable PhpClass phpClass) {

        if(phpClass == null) {
            return new PsiElement[0];
        }

        String phpClassName = phpClass.getPresentableFQN();
        if(phpClassName == null) {
            return new PsiElement[0];
        }

        Set<String> serviceNames = new ContainerCollectionResolver.ServiceCollector(phpClass.getProject(), ContainerCollectionResolver.Source.INDEX, ContainerCollectionResolver.Source.COMPILER).convertClassNameToServices(phpClassName);

        if(serviceNames.size() == 0) {
            return new PsiElement[0];
        }

        List<PsiElement> psiElements = new ArrayList<PsiElement>();
        for(String serviceName: serviceNames) {
            psiElements.addAll(findServiceDefinitions(phpClass.getProject(), serviceName));
        }

        return psiElements.toArray(new PsiElement[psiElements.size()]);
    }

    @Nullable
    public static ClassServiceDefinitionTargetLazyValue findServiceDefinitionsLazy(@Nullable PhpClass phpClass) {
        if(phpClass == null) {
            return null;
        }

        String phpClassName = phpClass.getPresentableFQN();
        if(phpClassName == null) {
            return null;
        }

        Set<String> serviceNames = new ContainerCollectionResolver.ServiceCollector(phpClass.getProject(), ContainerCollectionResolver.Source.INDEX, ContainerCollectionResolver.Source.COMPILER).convertClassNameToServices(phpClassName);
        if(serviceNames.size() == 0) {
            return null;
        }

        return new ClassServiceDefinitionTargetLazyValue(phpClass.getProject(), phpClassName);
    }

    /**
     * So support only some file types, so we can filter them and xml and yaml for now
     */
    public static GlobalSearchScope getRestrictedFileTypesScope(@NotNull Project project) {
        return GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), XmlFileType.INSTANCE, YAMLFileType.YML);
    }

}
