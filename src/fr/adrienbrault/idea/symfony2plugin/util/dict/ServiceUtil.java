package fr.adrienbrault.idea.symfony2plugin.util.dict;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlTagParser;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.SymfonyProcessors;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ContainerParameterStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ServicesTagStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLFileType;

import java.util.*;

public class ServiceUtil {


    public static final Map<String , String> TAG_INTERFACES = new HashMap<String , String>() {{
        put("assetic.asset", "\\Assetic\\Filter\\FilterInterface");
        put("assetic.factory_worker", "\\Assetic\\Factory\\Worker\\WorkerInterface");
        put("assetic.filter", "\\Assetic\\Filter\\FilterInterface");
        put("assetic.formula_loader", "\\Assetic\\Factory\\Loader\\FormulaLoaderInterface");
        put("assetic.formula_resource", null);
        put("assetic.templating.php", null);
        put("assetic.templating.twig", null);
        put("console.command", "\\Symfony\\Component\\Console\\Command\\Command");
        put("data_collector", "\\Symfony\\Component\\HttpKernel\\DataCollector\\DataCollectorInterface");
        put("doctrine.event_listener", null);
        put("doctrine.event_subscriber", null);
        put("form.type", "\\Symfony\\Component\\Form\\FormTypeInterface");
        put("form.type_extension", "\\Symfony\\Component\\Form\\FormTypeExtensionInterface");
        put("form.type_guesser", "\\Symfony\\Component\\Form\\FormTypeGuesserInterface");
        put("kernel.cache_clearer", null);
        put("kernel.cache_warmer", "\\Symfony\\Component\\HttpKernel\\CacheWarmer\\CacheWarmerInterface");
        put("kernel.event_subscriber", "\\Symfony\\Component\\EventDispatcher\\EventSubscriberInterface");
        put("kernel.fragment_renderer", "\\Symfony\\Component\\HttpKernel\\Fragment\\FragmentRendererInterface");
        put("monolog.logger", null);
        put("monolog.processor", null);
        put("routing.loader", "\\Symfony\\Component\\Config\\Loader\\LoaderInterface");
        //put("security.remember_me_aware", null);
        put("security.voter", "\\Symfony\\Component\\Security\\Core\\Authorization\\Voter\\VoterInterface");
        put("serializer.encoder", "\\Symfony\\Component\\Serializer\\Encoder\\EncoderInterface");
        put("serializer.normalizer", "\\Symfony\\Component\\Serializer\\Normalizer\\NormalizerInterface");
        // Symfony\Component\Serializer\Normalizer\DenormalizerInterface
        put("swiftmailer.default.plugin", "\\Swift_Events_EventListener");
        put("templating.helper", "\\Symfony\\Component\\Templating\\Helper\\HelperInterface");
        put("translation.loader", "\\Symfony\\Component\\Translation\\Loader\\LoaderInterface");
        put("translation.extractor", "\\Symfony\\Component\\Translation\\Extractor\\ExtractorInterface");
        put("translation.dumper", "\\Symfony\\Component\\Translation\\Dumper\\DumperInterface");
        put("twig.extension", "\\Twig_Extension");
        put("twig.loader", "\\Twig_LoaderInterface");
        put("validator.constraint_validator", "Symfony\\Component\\Validator\\ConstraintValidator");
        put("validator.initializer", "Symfony\\Component\\Validator\\ObjectInitializerInterface");

        // 2.6 - @TODO: how to handle duplicate interfaces; also make them weaker
        put("routing.expression_language_provider", "\\Symfony\\Component\\ExpressionLanguage\\ExpressionFunctionProviderInterface");
        put("security.expression_language_provider", "\\Symfony\\Component\\ExpressionLanguage\\ExpressionFunctionProviderInterface");
    }};

    /**
     * static event parameter list
     *
     * TODO: replace with live fetch
     */
    public static final Map<String , String> TAGS = new HashMap<String , String>() {{
        put("kernel.request", "\\Symfony\\Component\\HttpKernel\\Event\\GetResponseEvent");
        put("kernel.view", "\\Symfony\\Component\\HttpKernel\\Event\\GetResponseForControllerResultEvent");
        put("kernel.controller", "\\Symfony\\Component\\HttpKernel\\Event\\FilterControllerEvent");
        put("kernel.response", "\\Symfony\\Component\\HttpKernel\\Event\\FilterResponseEvent");
        put("kernel.finish_request", "\\Symfony\\Component\\HttpKernel\\Event\\FinishRequestEvent");
        put("kernel.terminate", "\\Symfony\\Component\\HttpKernel\\Event\\PostResponseEvent");
        put("kernel.exception", "\\Symfony\\Component\\HttpKernel\\Event\\GetResponseForExceptionEvent");
        put("console.command", "\\Symfony\\Component\\Console\\Event\\ConsoleCommandEvent");
        put("console.terminate", "\\Symfony\\Component\\Console\\Event\\ConsoleTerminateEvent");
        put("console.exception", "\\Symfony\\Component\\Console\\Event\\ConsoleExceptionEvent");
        put("form.pre_bind", "\\Symfony\\Component\\Form\\FormEvent");
        put("form.bind", "\\Symfony\\Component\\Form\\FormEvent");
        put("form.post_bind", "\\Symfony\\Component\\Form\\FormEvent");
        put("form.pre_set_data", "\\Symfony\\Component\\Form\\FormEvent");
        put("form.post_set_data", "\\Symfony\\Component\\Form\\FormEvent");
    }};

    /**
     * %test%, service, \Class\Name to PhpClass
     */
    @Nullable
    public static PhpClass getResolvedClassDefinition(@NotNull Project project, @NotNull String serviceClassParameterName) {
        return getResolvedClassDefinition(project, serviceClassParameterName, new ContainerCollectionResolver.LazyServiceCollector(project));
    }

    /**
     * %test%, service, \Class\Name to PhpClass
     */
    @Nullable
    public static PhpClass getResolvedClassDefinition(@NotNull Project project, @NotNull String serviceClassParameterName, ContainerCollectionResolver.LazyServiceCollector collector) {

        // match parameter
        if(serviceClassParameterName.startsWith("%") && serviceClassParameterName.endsWith("%")) {
            String serviceClass = ContainerCollectionResolver.resolveParameter(project, serviceClassParameterName);

            if(serviceClass != null) {
                return PhpElementsUtil.getClassInterface(project, serviceClass);
            }

            return null;
        }

        // service names dont have namespaces
        if(!serviceClassParameterName.contains("\\")) {
            String serviceClass = collector.getCollector().resolve(serviceClassParameterName);
            if (serviceClass != null) {
                return PhpElementsUtil.getClassInterface(project, serviceClass);
            }
        }

        // fallback to class name with and without namespaces
        return PhpElementsUtil.getClassInterface(project, serviceClassParameterName);
    }


    /**
     * Get parameter def inside xml or yaml file
     */
    public static Collection<PsiElement> getParameterDefinition(Project project, String parameterName) {

        if(parameterName.length() > 2 && parameterName.startsWith("%") && parameterName.endsWith("%")) {
            parameterName = parameterName.substring(1, parameterName.length() - 1);
        }

        Collection<PsiElement> psiElements = new ArrayList<PsiElement>();

        Collection<VirtualFile> fileCollection = FileBasedIndex.getInstance().getContainingFiles(ContainerParameterStubIndex.KEY, parameterName, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), XmlFileType.INSTANCE, YAMLFileType.YML));
        for(VirtualFile virtualFile: fileCollection) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if(psiFile != null) {
                psiElements.addAll(ServiceIndexUtil.findParameterDefinitions(psiFile, parameterName));
            }
        }

        return psiElements;

    }

    public static Collection<PsiElement> getServiceClassTargets(@NotNull Project project, @Nullable String value) {

        List<PsiElement> resolveResults = new ArrayList<PsiElement>();

        if(value == null || StringUtils.isBlank(value)) {
            return resolveResults;
        }

        // resolve class or parameter class
        PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(project, value);
        if(phpClass != null) {
            resolveResults.add(phpClass);
        }

        // get parameter def target
        if(value.startsWith("%") && value.endsWith("%")) {
            resolveResults.addAll(ServiceUtil.getParameterDefinition(project, value));
        }

        return resolveResults;
    }

    public static Set<String> getTaggedServices(Project project, String tagName) {

        SymfonyProcessors.CollectProjectUniqueKeys projectUniqueKeysStrong = new SymfonyProcessors.CollectProjectUniqueKeys(project, ServicesTagStubIndex.KEY);
        FileBasedIndexImpl.getInstance().processAllKeys(ServicesTagStubIndex.KEY, projectUniqueKeysStrong, project);

        Set<String> service = new HashSet<String>();

        for(String serviceName: projectUniqueKeysStrong.getResult()) {

            List<String[]> serviceDefinitions = FileBasedIndexImpl.getInstance().getValues(ServicesTagStubIndex.KEY, serviceName, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), XmlFileType.INSTANCE, YAMLFileType.YML));
            for(String[] strings: serviceDefinitions) {
                if(Arrays.asList(strings).contains(tagName)) {
                    service.add(serviceName);
                }
            }

        }

        return service;
    }

    public static Collection<PhpClass> getTaggedClasses(Project project, String tagName) {

        List<PhpClass> phpClasses = new ArrayList<PhpClass>();

        Set<String> taggedServices = getTaggedServices(project, tagName);
        if(taggedServices.size() == 0) {
            return phpClasses;
        }

        ContainerCollectionResolver.ServiceCollector collector = new ContainerCollectionResolver.ServiceCollector(project, ContainerCollectionResolver.Source.COMPILER, ContainerCollectionResolver.Source.INDEX);
        for(String serviceName: taggedServices) {
            String resolvedService = collector.resolve(serviceName);
            if(resolvedService != null) {
                PhpClass phpClass = PhpElementsUtil.getClass(project, resolvedService);
                if(phpClass != null) {
                    phpClasses.add(phpClass);
                }
            }
        }

        return phpClasses;
    }

    public static Collection<PhpClass> getTaggedClassesWithCompiled(Project project, String tagName) {

        Set<String> uniqueClass = new HashSet<String>();

        Collection<PhpClass> taggedClasses = new ArrayList<PhpClass>();
        for(PhpClass phpClass: getTaggedClasses(project, tagName)) {
            String presentableFQN = phpClass.getPresentableFQN();
            if(presentableFQN != null && !uniqueClass.contains(presentableFQN)) {
                uniqueClass.add(presentableFQN);
                taggedClasses.add(phpClass);
            }
        }

        XmlTagParser xmlTagParser = ServiceXmlParserFactory.getInstance(project, XmlTagParser.class);

        List<String> taggedCompiledClasses= xmlTagParser.getTaggedClass(tagName);
        if(taggedCompiledClasses == null) {
            return taggedClasses;
        }

        for(String className: taggedCompiledClasses) {
            if(!uniqueClass.contains(className)) {
                PhpClass phpClass = PhpElementsUtil.getClass(project, className);
                if(phpClass != null) {
                    taggedClasses.add(phpClass);
                }
            }
        }

        return taggedClasses;
    }


    /**
     * Resolve "@service" to its class
     */
    @Nullable
    public static PhpClass getServiceClass(@NotNull Project project, @NotNull String serviceName) {

        serviceName = YamlHelper.trimSpecialSyntaxServiceName(serviceName);

        if(serviceName.length() == 0) {
            return null;
        }

        ContainerService containerService = ContainerCollectionResolver.getService(project, serviceName);
        if(containerService == null) {
            return null;
        }

        String serviceClass = containerService.getClassName();
        if(serviceClass == null) {
            return null;
        }

        return PhpElementsUtil.getClassInterface(project, serviceClass);
    }

    /**
     *  Gets all tags on extends/implements path of class
     */
    @NotNull
    public static Set<String> getPhpClassServiceTags(@NotNull PhpClass phpClass) {

        Set<String> tags = new HashSet<String>();

        Symfony2InterfacesUtil symfony2InterfacesUtil = new Symfony2InterfacesUtil();

        for (Map.Entry<String, String> entry : TAG_INTERFACES.entrySet()) {

            if(entry.getValue() == null) {
                continue;
            }

            if(symfony2InterfacesUtil.isInstanceOf(phpClass, entry.getValue())) {
                tags.add(entry.getKey());
            }

        }

        return tags;
    }

    /**
     * Event based decoration of class
     */
    public static void decorateServiceTag(@NotNull ServiceTag service) {

        // @TODO: provide extension
        // form alias
        if(service.getTagName().equals("form.type") && new Symfony2InterfacesUtil().isInstanceOf(service.getPhpClass(), FormUtil.ABSTRACT_FORM_INTERFACE)) {
            Set<String> aliases = FormUtil.getFormAliases(service.getPhpClass());
            if(aliases.size() > 0) {
                service.addAttribute("alias", aliases.iterator().next());
            }
        }
    }

}
