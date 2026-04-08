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
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.ClassServiceDefinitionTargetLazyValue;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceSerializable;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.extension.ServiceDefinitionLocator;
import fr.adrienbrault.idea.symfony2plugin.extension.ServiceDefinitionLocatorParameter;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ServicesDefinitionStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.*;
import java.util.function.Supplier;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceIndexUtil {

    private static final Key<CachedValue<Map<String, Collection<ContainerService>>>> SERVICE_DECORATION_CACHE = new Key<>("SERVICE_DECORATION");
    private static final Key<CachedValue<Map<String, Collection<ContainerService>>>> SERVICE_PARENT = new Key<>("SERVICE_PARENT");

    private static final ExtensionPointName<ServiceDefinitionLocator> EXTENSIONS = new ExtensionPointName<>(
        "fr.adrienbrault.idea.symfony2plugin.extension.ServiceDefinitionLocator"
    );

    public static VirtualFile[] findServiceDefinitionFiles(@NotNull Project project, @NotNull String serviceName) {

        final List<VirtualFile> virtualFiles = new ArrayList<>();

        FileBasedIndex.getInstance().getFilesWithKey(ServicesDefinitionStubIndex.KEY, new HashSet<>(Collections.singletonList(serviceName.toLowerCase())), virtualFile -> {
            virtualFiles.add(virtualFile);
            return true;
        }, getRestrictedFileTypesScope(project));

        return virtualFiles.toArray(new VirtualFile[0]);

    }

    @Nullable
    public static ServiceSerializable findServiceDefinition(@NotNull Project project, @NotNull VirtualFile virtualFile, @NotNull String serviceName) {
        return FileBasedIndex.getInstance()
            .getFileData(ServicesDefinitionStubIndex.KEY, virtualFile, project)
            .get(serviceName.toLowerCase());
    }

    /**
     * Expand resource/exclude patterns for an indexed service definition into PhpClass targets.
     * Used by XML, YAML, and PHP line marker providers to resolve "Navigate to class" gutters.
     */
    @NotNull
    public static Collection<PsiElement> getClassesForServiceDefinition(
        @NotNull Project project,
        @NotNull VirtualFile containerFile,
        @NotNull ServiceSerializable service
    ) {
        Collection<PsiElement> targets = new HashSet<>();

        for (String className : ServiceContainerUtil.getPhpClassFromResources(
            project,
            service.getId(),
            containerFile,
            service.getResource(),
            service.getExclude()
        )) {
            targets.addAll(PhpElementsUtil.getClassesInterface(project, className));
        }

        return targets;
    }

    public static List<PsiElement> findServiceDefinitions(@NotNull Project project, @NotNull String serviceName) {
        return findServiceDefinitions(project, serviceName, false);
    }

    @NotNull
    public static List<PsiElement> findServiceDefinitionBlocks(@NotNull Project project, @NotNull String serviceName) {
        return findServiceDefinitions(project, serviceName, true);
    }

    @NotNull
    private static List<PsiElement> findServiceDefinitions(@NotNull Project project, @NotNull String serviceName, boolean usePhpAttributePsi) {

        List<PsiElement> items = new ArrayList<>();

        VirtualFile[] twigVirtualFiles = ServiceIndexUtil.findServiceDefinitionFiles(project, serviceName);

        for (VirtualFile twigVirtualFile : twigVirtualFiles) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(twigVirtualFile);

            if(psiFile instanceof YAMLFile) {
                PsiElement servicePsiElement = YamlHelper.getLocalServiceName(psiFile, serviceName);
                if(servicePsiElement != null) {
                    items.add(servicePsiElement);
                }
            } else if(psiFile instanceof XmlFile) {
                PsiElement servicePsiElement = XmlHelper.getLocalServiceName(psiFile, serviceName);
                if(servicePsiElement != null) {
                    items.add(servicePsiElement);
                }
            } else if(psiFile instanceof PhpFile) {
                ServiceContainerUtil.visitFile((PhpFile) psiFile, service -> {
                    if (serviceName.equalsIgnoreCase(service.getServiceId())) {
                        items.add(usePhpAttributePsi ? service.attributes().getPsiElement() : service.getPsiElement());
                    }
                });
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

        if(serviceNames.isEmpty()) {
            return new PsiElement[0];
        }

        List<PsiElement> psiElements = new ArrayList<>();
        for(String serviceName: serviceNames) {
            psiElements.addAll(findServiceDefinitions(phpClass.getProject(), serviceName));
        }

        return psiElements.toArray(new PsiElement[0]);
    }

    /**
     * Lazy values for linemarker
     */
    @NotNull
    public static NotNullLazyValue<Collection<? extends PsiElement>> getServiceIdDefinitionLazyValue(@NotNull Project project, @NotNull Collection<String> ids) {
        return NotNullLazyValue.lazy(new MyServiceIdLazyValue(project, ids));
    }

    /**
     * So support only some file types, so we can filter them and xml and yaml for now
     */
    public static GlobalSearchScope getRestrictedFileTypesScope(@NotNull Project project) {
        return GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), XmlFileType.INSTANCE, YAMLFileType.YML, PhpFileType.INSTANCE);
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
            for (String decorates : containerService.getDecoratesValues()) {
                services.computeIfAbsent(decorates, key -> new ArrayList<>()).add(containerService);
            }
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
            for (String parent : containerService.getParents()) {
                services.computeIfAbsent(parent, key -> new ArrayList<>()).add(containerService);
            }
        }

        return services;
    }

    private record MyServiceIdLazyValue(@NotNull Project project, @NotNull Collection<String> ids) implements Supplier<Collection<? extends PsiElement>> {
        @Override
        public Collection<? extends PsiElement> get() {
            Collection<PsiElement> psiElements = new HashSet<>();

            for (String id : new HashSet<>(this.ids)) {
                psiElements.addAll(findServiceDefinitions(this.project, id));
            }

            return psiElements;
        }
    }
}
