package fr.adrienbrault.idea.symfony2plugin.util.dict;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.*;
import com.intellij.ui.components.JBList;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfony2plugin.action.generator.naming.DefaultServiceNameStrategy;
import fr.adrienbrault.idea.symfony2plugin.action.generator.naming.JavascriptServiceNameStrategy;
import fr.adrienbrault.idea.symfony2plugin.action.generator.naming.ServiceNameStrategyInterface;
import fr.adrienbrault.idea.symfony2plugin.action.generator.naming.ServiceNameStrategyParameter;
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

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceUtil {
    private static ServiceNameStrategyInterface[] NAME_STRATEGIES = new ServiceNameStrategyInterface[] {
        new JavascriptServiceNameStrategy(),
        new DefaultServiceNameStrategy(),
    };

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
        put("twig.extension", "\\Twig_ExtensionInterface");
        put("twig.loader", "\\Twig_LoaderInterface");
        put("validator.constraint_validator", "Symfony\\Component\\Validator\\ConstraintValidator");
        put("validator.initializer", "Symfony\\Component\\Validator\\ObjectInitializerInterface");

        // 2.6 - @TODO: how to handle duplicate interfaces; also make them weaker
        put("routing.expression_language_provider", "\\Symfony\\Component\\ExpressionLanguage\\ExpressionFunctionProviderInterface");
        put("security.expression_language_provider", "\\Symfony\\Component\\ExpressionLanguage\\ExpressionFunctionProviderInterface");

        put("controller.service_arguments", null);
    }};

    /**
     * static event parameter list
     *
     * TODO: replace with live fetch; now redundant because of @Event const in Symfony itself
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
     * Cache kernel.root_dir of Kernel class
     */
    private static final Key<CachedValue<Collection<String>>> KERNEL_PARAMETER_CACHE = new Key<>("KERNEL_PARAMETER_CACHE");

    /**
     * Linemarker types for service declarations
     */
    public enum ServiceLineMarker {
        DECORATE, PARENT
    }

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
            String serviceClass = ContainerCollectionResolver.resolveParameter(collector.getParameterCollector(), serviceClassParameterName);

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

        Collection<PsiElement> psiElements = new ArrayList<>();

        Collection<VirtualFile> fileCollection = FileBasedIndex.getInstance().getContainingFiles(ContainerParameterStubIndex.KEY, parameterName, ServiceIndexUtil.getRestrictedFileTypesScope(project));
        for(VirtualFile virtualFile: fileCollection) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if(psiFile != null) {
                psiElements.addAll(ServiceIndexUtil.findParameterDefinitions(psiFile, parameterName));
            }
        }

        return psiElements;

    }

    public static Collection<PsiElement> getServiceClassTargets(@NotNull Project project, @Nullable String value) {

        List<PsiElement> resolveResults = new ArrayList<>();

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

    /**
     * Find every service tag that's implements or extends a classes/interface of give class
     */
    @NotNull
    public static Set<String> getPhpClassTags(@NotNull PhpClass phpClass) {
        Project project = phpClass.getProject();

        ContainerCollectionResolver.ServiceCollector collector = null;

        Set<String> matchedTags = new HashSet<>();
        Set<String> result = SymfonyProcessors.createResult(project, ServicesTagStubIndex.KEY);
        for (String serviceName : result) {

            // get service where we found our tags
            List<Set<String>> values = FileBasedIndex.getInstance().getValues(ServicesTagStubIndex.KEY, serviceName, ServiceIndexUtil.getRestrictedFileTypesScope(project));
            if(values.size() == 0) {
                continue;
            }

            // create unique tag list
            Set<String> tags = new HashSet<>();
            for(Set<String> tagValue: values) {
                tags.addAll(tagValue);
            }

            if(collector == null) {
                collector = ContainerCollectionResolver.ServiceCollector.create(project);
            }

            PhpClass serviceClass = ServiceUtil.getServiceClass(project, serviceName, collector);
            if(serviceClass == null) {
                continue;
            }

            boolean matched = false;

            // get classes this service implements or extends
            for (PhpClass serviceClassImpl: getSuperClasses(serviceClass)) {
                // find interface or extends class which also implements
                // @TODO: currently first level only, check recursive
                if(!PhpElementsUtil.isEqualClassName(phpClass, serviceClassImpl) && PhpElementsUtil.isInstanceOf(phpClass, serviceClassImpl)) {
                    matched = true;
                    break;
                }
            }

            if(matched) {
                matchedTags.addAll(tags);
            }
        }

        return matchedTags;
    }

    /**
     * Get "extends" and implements on class level
     */
    @NotNull
    public static Set<PhpClass> getSuperClasses(@NotNull PhpClass serviceClass) {
        Set<PhpClass> phpClasses = new HashSet<>();
        PhpClass superClass = serviceClass.getSuperClass();

        if(superClass != null)  {
            phpClasses.add(superClass);
        }

        phpClasses.addAll(Arrays.asList(serviceClass.getImplementedInterfaces()));

        return phpClasses;
    }

    public static Set<String> getTaggedServices(Project project, String tagName) {
        Set<String> service = new HashSet<>();

        for(String serviceName: SymfonyProcessors.createResult(project, ServicesTagStubIndex.KEY)) {
            List<Set<String>> serviceDefinitions = FileBasedIndex.getInstance().getValues(ServicesTagStubIndex.KEY, serviceName, ServiceIndexUtil.getRestrictedFileTypesScope(project));
            for(Set<String> strings: serviceDefinitions) {
                if(strings.contains(tagName)) {
                    service.add(serviceName);
                }
            }
        }

        return service;
    }

    @NotNull
    public static Set<String> getServiceTags(@NotNull Project project, @NotNull String serviceId) {
        Set<String> service = new HashSet<>();

        for(Set<String> strings: FileBasedIndex.getInstance().getValues(ServicesTagStubIndex.KEY, serviceId, ServiceIndexUtil.getRestrictedFileTypesScope(project))) {
            service.addAll(strings);
        }

        for(Set<String> strings: FileBasedIndex.getInstance().getValues(ServicesTagStubIndex.KEY, serviceId.toLowerCase(), ServiceIndexUtil.getRestrictedFileTypesScope(project))) {
            service.addAll(strings);
        }

        return service;
    }

    public static Collection<PhpClass> getTaggedClasses(@NotNull Project project, @NotNull String tagName) {

        List<PhpClass> phpClasses = new ArrayList<>();

        Set<String> taggedServices = getTaggedServices(project, tagName);
        if(taggedServices.size() == 0) {
            return phpClasses;
        }

        ContainerCollectionResolver.ServiceCollector collector = ContainerCollectionResolver.ServiceCollector.create(project);
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

    @NotNull
    public static Collection<PhpClass> getTaggedClassesWithCompiled(@NotNull Project project, @NotNull String tagName) {

        Set<String> uniqueClass = new HashSet<>();

        Collection<PhpClass> taggedClasses = new ArrayList<>();
        for(PhpClass phpClass: getTaggedClasses(project, tagName)) {
            String presentableFQN = phpClass.getPresentableFQN();
            if(!uniqueClass.contains(presentableFQN)) {
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
     * Resolve "@service" to its class + proxy ServiceCollector for iteration
     */
    @Nullable
    public static PhpClass getServiceClass(@NotNull Project project, @NotNull String serviceName, @NotNull ContainerCollectionResolver.ServiceCollector collector) {

        serviceName = YamlHelper.trimSpecialSyntaxServiceName(serviceName);
        if(serviceName.length() == 0) {
            return null;
        }

        String resolve = collector.resolve(serviceName);
        if(resolve == null) {
            return null;
        }

        return PhpElementsUtil.getClassInterface(project, resolve);
    }

    /**
     *  Gets all tags on extends/implements path of class
     */
    @NotNull
    public static Set<String> getPhpClassServiceTags(@NotNull PhpClass phpClass) {

        Set<String> tags = new HashSet<>();

        for (Map.Entry<String, String> entry : TAG_INTERFACES.entrySet()) {

            if(entry.getValue() == null) {
                continue;
            }

            if(PhpElementsUtil.isInstanceOf(phpClass, entry.getValue())) {
                tags.add(entry.getKey());
            }

        }

        // strong tags wins
        if(tags.size() > 0) {
            return tags;
        }

        // try to resolve on indexed tags
        return getPhpClassTags(phpClass);
    }

    /**
     * Event based decoration of class
     */
    public static void decorateServiceTag(@NotNull ServiceTag service) {

        // @TODO: provide extension
        // form alias
        if(service.getTagName().equals("form.type") && PhpElementsUtil.isInstanceOf(service.getPhpClass(), FormUtil.ABSTRACT_FORM_INTERFACE)) {
            Collection<String> aliases = FormUtil.getFormAliases(service.getPhpClass());
            if(aliases.size() > 0) {
                service.addAttribute("alias", aliases.iterator().next());
            }
        }
    }

    @NotNull
    public static Collection<ContainerService> getServiceSuggestionForPhpClass(@NotNull PhpClass phpClass, @NotNull Map<String, ContainerService> serviceMap) {
        return getServiceSuggestionForPhpClass(phpClass, serviceMap.values());
    }

    @NotNull
    public static Collection<ContainerService> getServiceSuggestionForPhpClass(@NotNull PhpClass phpClass, @NotNull Collection<ContainerService> serviceMap) {

        String fqn = StringUtils.stripStart(phpClass.getFQN(), "\\");

        Collection<ContainerService> instances = new ArrayList<>();

        for(ContainerService service: serviceMap) {
            if(service.getClassName() == null) {
                continue;
            }

            PhpClass serviceClass = PhpElementsUtil.getClassInterface(phpClass.getProject(), service.getClassName());
            if(serviceClass == null) {
                continue;
            }

            if(PhpElementsUtil.isInstanceOf(serviceClass, fqn)) {
                instances.add(service);
            }
        }

        return instances;
    }

    @NotNull
    public static Set<String> getServiceSuggestionsForServiceConstructorIndex(@NotNull Project project, @NotNull String serviceName, int index) {
        PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(project, serviceName);
        // check type hint on constructor
        if(phpClass == null) {
            return Collections.emptySet();
        }

        Method constructor = phpClass.getConstructor();
        if(constructor == null) {
            return Collections.emptySet();
        }

        Parameter[] constructorParameter = constructor.getParameters();
        if(index >= constructorParameter.length) {
            return Collections.emptySet();
        }

        String className = constructorParameter[index].getDeclaredType().toString();
        PhpClass expectedClass = PhpElementsUtil.getClassInterface(project, className);
        if(expectedClass == null) {
            return Collections.emptySet();
        }

        return ServiceActionUtil.getPossibleServices(expectedClass, ContainerCollectionResolver.getServices(project));
    }

    public static void insertTagWithPopupDecision(final @NotNull Editor editor, final @NotNull Set<String> phpServiceTags, final @NotNull Consumer<String> consumer) {
        final JBList<String> list = new JBList<>(phpServiceTags);

        if(phpServiceTags.size() == 0) {
            HintManager.getInstance().showErrorHint(editor, "Ops, no tag found");
            return;
        }

        if(phpServiceTags.size() == 1) {
            new WriteCommandAction.Simple(editor.getProject(), "Service Suggestion Insert") {
                @Override
                protected void run() {
                    consumer.consume(phpServiceTags.iterator().next());
                }
            }.execute();

            return;
        }

        JBPopupFactory.getInstance().createListPopupBuilder(list)
            .setTitle("Symfony: Tag Suggestion")
            .setItemChoosenCallback(() -> new WriteCommandAction.Simple(editor.getProject(), "Service Suggestion Insert") {
                @Override
                protected void run() {
                    consumer.consume((String) list.getSelectedValue());
                }
            }.execute())
            .createPopup()
            .showInBestPositionFor(editor);
    }

    @NotNull
    public static Collection<ContainerService> getServiceSuggestionsForTypeHint(@NotNull Method method, int index, @NotNull Collection<ContainerService> services) {
        PhpClass phpClass = PhpElementsUtil.getMethodTypeHintParameterPhpClass(method, index);
        if(phpClass == null) {
            return Collections.emptyList();
        }

        return ServiceUtil.getServiceSuggestionForPhpClass(phpClass, services);
    }

    @NotNull
    public static String getServiceNameForClass(@NotNull Project project, @NotNull String className) {
        className = StringUtils.stripStart(className, "\\");

        ServiceNameStrategyParameter parameter = new ServiceNameStrategyParameter(project, className);
        for (ServiceNameStrategyInterface nameStrategy : NAME_STRATEGIES) {
            String serviceName = nameStrategy.getServiceName(parameter);
            if(serviceName != null && StringUtils.isNotBlank(serviceName)) {
                return serviceName;
            }
        }

        return className.toLowerCase().replace("\\", "_");
    }

    @Nullable
    public static NavigationGutterIconBuilder<PsiElement> getLineMarkerForDecoratedServiceId(@NotNull Project project, @NotNull ServiceLineMarker lineMarker, @NotNull Map<String, Collection<ContainerService>> decorated, @NotNull String id) {
        if(!decorated.containsKey(id)) {
            return null;
        }

        NotNullLazyValue<Collection<? extends PsiElement>> lazy = ServiceIndexUtil.getServiceIdDefinitionLazyValue(
            project,
            ContainerUtil.map(decorated.get(id), ContainerService::getName)
        );

        return NavigationGutterIconBuilder.create(PhpIcons.IMPLEMENTS)
            .setTargets(lazy)
            .setTooltipText(lineMarker == ServiceLineMarker.DECORATE ? "Navigate to decoration" : "Navigate to parent" );
    }

    /**
     * Extract parameter on php file
     *
     * return array_merge(
     *  array(
     *      'kernel.root_dir' => realpath($this->rootDir) ?: $this->rootDir,
     *      'kernel.project_dir' => realpath($this->getProjectDir()) ?: $this->getProjectDir(),
     *  ),
     *  $this->getEnvParameters(false)
     *  );
     */
    @NotNull
    public static Collection<String> getParameterParameters(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            KERNEL_PARAMETER_CACHE,
            () -> CachedValueProvider.Result.create(getParameterParametersInner(project), PsiModificationTracker.MODIFICATION_COUNT),
            false
        );
    }

    @NotNull
    private static Collection<String> getParameterParametersInner(@NotNull Project project) {
        Collection<String> parameters = new HashSet<>();

        Collection<PhpClass> phpClasses = new HashSet<PhpClass>() {{
            addAll(PhpIndex.getInstance(project).getAnyByFQN("Symfony\\Component\\HttpKernel\\Kernel"));
            addAll(PhpIndex.getInstance(project).getAllSubclasses("Symfony\\Component\\HttpKernel\\Kernel"));
        }};

        for (PhpClass phpClass : phpClasses) {
            Method method = phpClass.findMethodByName("getKernelParameters");
            if(method == null) {
                continue;
            }

            // search for for all return values and try to extract array keys
            for (PhpReturn phpReturn: PsiTreeUtil.collectElementsOfType(method, PhpReturn.class)) {
                PhpPsiElement firstPsiChild = phpReturn.getFirstPsiChild();

                if(firstPsiChild instanceof ArrayCreationExpression) {
                    // return ['foobar' => 'foo']

                    parameters.addAll(PhpElementsUtil.getArrayCreationKeys((ArrayCreationExpression) firstPsiChild));
                } else if(firstPsiChild instanceof FunctionReference && (
                    "array_merge".equalsIgnoreCase(firstPsiChild.getName()) ||
                    "array_merge_recursive".equalsIgnoreCase(firstPsiChild.getName()) ||
                    "array_replace".equalsIgnoreCase(firstPsiChild.getName())
                )) {
                    // return array_merge(['foobar' => 'foo'])
                    // return array_merge($foobar, ['foobar' => 'foo'])

                    for (PsiElement parameter : ((FunctionReference) firstPsiChild).getParameters()) {
                        if(parameter instanceof ArrayCreationExpression) {
                            parameters.addAll(PhpElementsUtil.getArrayCreationKeys((ArrayCreationExpression) parameter));
                        }
                    }
                }
            }
        }

        return parameters;
    }

    /**
     * Provides a lazy linemarker based on the given id eg for "decorated" or "parent" services:
     *
     * <service id="foo_bar_main" decorates="app.mailer"/>
     */
    @NotNull
    public static RelatedItemLineMarkerInfo<PsiElement> getLineMarkerForDecoratesServiceId(@NotNull PsiElement psiElement, @NotNull ServiceLineMarker lineMarker, @NotNull String foreignId) {
        return NavigationGutterIconBuilder.create(PhpIcons.OVERRIDEN)
            .setTargets(ServiceIndexUtil.getServiceIdDefinitionLazyValue(psiElement.getProject(), Collections.singletonList(foreignId)))
            .setTooltipText(lineMarker == ServiceLineMarker.DECORATE ? "Navigate to decorated service" : "Navigate to parent service")
            .createLineMarkerInfo(psiElement);
    }
}
