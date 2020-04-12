package fr.adrienbrault.idea.symfony2plugin.stubs;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.ClassServiceDefinitionTargetLazyValue;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.extension.ServiceDefinitionLocator;
import fr.adrienbrault.idea.symfony2plugin.extension.ServiceDefinitionLocatorParameter;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ServicesDefinitionStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceIndexUtil {

    private static final Key<CachedValue<Map<String, Collection<ContainerService>>>> SERVICE_DECORATION_CACHE = new Key<>("SERVICE_DECORATION");
    private static final Key<CachedValue<Map<String, Collection<ContainerService>>>> SERVICE_PARENT = new Key<>("SERVICE_PARENT");

    private static final ExtensionPointName<ServiceDefinitionLocator> EXTENSIONS = new ExtensionPointName<>(
        "fr.adrienbrault.idea.symfony2plugin.extension.ServiceDefinitionLocator"
    );

    private static VirtualFile[] findServiceDefinitionFiles(@NotNull Project project, @NotNull String serviceName) {

        final List<VirtualFile> virtualFiles = new ArrayList<>();

        FileBasedIndex.getInstance().getFilesWithKey(ServicesDefinitionStubIndex.KEY, new HashSet<>(Collections.singletonList(serviceName.toLowerCase())), virtualFile -> {
            virtualFiles.add(virtualFile);
            return true;
        }, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), XmlFileType.INSTANCE, YAMLFileType.YML));

        return virtualFiles.toArray(new VirtualFile[virtualFiles.size()]);

    }

    public static List<PsiElement> findServiceDefinitions(@NotNull Project project, @NotNull String serviceName) {

        List<PsiElement> items = new ArrayList<>();

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

        List<PsiElement> items = new ArrayList<>();

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
        Set<String> serviceNames = ContainerCollectionResolver.ServiceCollector.create(phpClass.getProject()).convertClassNameToServices(phpClassName);

        if(serviceNames.size() == 0) {
            return new PsiElement[0];
        }

        List<PsiElement> psiElements = new ArrayList<>();
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
        Set<String> serviceNames = ContainerCollectionResolver.ServiceCollector.create(phpClass.getProject()).convertClassNameToServices(phpClassName);
        if(serviceNames.size() == 0) {
            return null;
        }

        return new ClassServiceDefinitionTargetLazyValue(phpClass.getProject(), phpClassName);
    }

    /**
     * Lazy values for linemarker
     */
    @NotNull
    public static NotNullLazyValue<Collection<? extends PsiElement>> getServiceIdDefinitionLazyValue(@NotNull Project project, @NotNull Collection<String> ids) {
        return new MyServiceIdLazyValue(project, ids);
    }

    /**
     * So support only some file types, so we can filter them and xml and yaml for now
     */
    public static GlobalSearchScope getRestrictedFileTypesScope(@NotNull Project project) {
        return GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), XmlFileType.INSTANCE, YAMLFileType.YML);
    }

    @NotNull
    public static Map<String, Collection<ContainerService>> getDecoratedServices(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            SERVICE_DECORATION_CACHE,
            () -> CachedValueProvider.Result.create(getDecoratedServicesInner(project), PsiModificationTracker.MODIFICATION_COUNT),
            false
        );
    }

    @NotNull
    private static Map<String, Collection<ContainerService>> getDecoratedServicesInner(@NotNull Project project) {
        Map<String, Collection<ContainerService>> services = new HashMap<>();

        for (ContainerService containerService : ContainerCollectionResolver.getServices(project).values()) {
            if(containerService.getService() == null) {
                continue;
            }

            String decorates = containerService.getService().getDecorates();
            if(decorates == null) {
                continue;
            }

            if(!services.containsKey(decorates)) {
                services.put(decorates, new ArrayList<>());
            }

            services.get(decorates).add(containerService);
        }

        return services;
    }

    /**
     * Get all services that extends a given "parent" id
     */
    @NotNull
    public static Map<String, Collection<ContainerService>> getParentServices(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            SERVICE_PARENT,
            () -> CachedValueProvider.Result.create(getParentServicesInner(project), PsiModificationTracker.MODIFICATION_COUNT),
            false
        );
    }

    /**
     * All services "parents" in cached condition
     */
    @NotNull
    private static Map<String, Collection<ContainerService>> getParentServicesInner(@NotNull Project project) {
        Map<String, Collection<ContainerService>> services = new HashMap<>();

        for (ContainerService containerService : ContainerCollectionResolver.getServices(project).values()) {
            if(containerService.getService() == null) {
                continue;
            }

            String parent = containerService.getService().getParent();
            if(parent == null) {
                continue;
            }

            if(!services.containsKey(parent)) {
                services.put(parent, new ArrayList<>());
            }

            services.get(parent).add(containerService);
        }

        return services;
    }

    private static class MyServiceIdLazyValue extends NotNullLazyValue<Collection<? extends PsiElement>> {
        @NotNull
        private final Project project;

        @NotNull
        private final Collection<String> ids;

        MyServiceIdLazyValue(@NotNull Project project, @NotNull Collection<String> ids) {
            this.project = project;
            this.ids = ids;
        }

        @NotNull
        @Override
        protected Collection<? extends PsiElement> compute() {
            Collection<PsiElement> psiElements = new HashSet<>();

            for (String id : new HashSet<>(this.ids)) {
                psiElements.addAll(findServiceDefinitions(this.project, id));
            }

            return psiElements;
        }
    }
}
