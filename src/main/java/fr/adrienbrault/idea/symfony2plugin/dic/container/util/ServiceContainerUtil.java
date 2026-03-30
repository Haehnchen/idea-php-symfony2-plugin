package fr.adrienbrault.idea.symfony2plugin.dic.container.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.util.*;
import com.intellij.psi.xml.*;
import com.intellij.util.Consumer;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.codeInsight.PhpScopeHolder;
import com.jetbrains.php.codeInsight.controlFlow.PhpControlFlowUtil;
import com.jetbrains.php.codeInsight.controlFlow.PhpInstructionProcessor;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpReturnInstruction;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpClassFqnIndex;
import com.jetbrains.php.refactoring.PhpNamespaceBraceConverter;
import fr.adrienbrault.idea.symfony2plugin.config.php.PhpArrayServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.attribute.value.AttributeValueInterface;
import fr.adrienbrault.idea.symfony2plugin.dic.attribute.value.PhpKeyValueAttributeValue;
import fr.adrienbrault.idea.symfony2plugin.dic.attribute.value.XmlTagAttributeValue;
import fr.adrienbrault.idea.symfony2plugin.dic.attribute.value.YamlKeyValueAttributeValue;
import fr.adrienbrault.idea.symfony2plugin.dic.container.SerializableService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceSerializable;
import fr.adrienbrault.idea.symfony2plugin.dic.container.dict.ServiceFileDefaults;
import fr.adrienbrault.idea.symfony2plugin.dic.container.dict.ServiceTypeHint;
import fr.adrienbrault.idea.symfony2plugin.dic.container.visitor.ServiceConsumer;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceResourceGlobMatcher;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ContainerIdUsagesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.*;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PsiElementAssertUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceContainerUtil {
    private static final Key<CachedValue<Map<String, Collection<PhpClassResourceCandidate>>>> PHP_CLASS_FQN_RESOURCE_CACHE = new Key<>("SYMFONY_PHP_CLASS_FQN_RESOURCE_CACHE");
    public static final MethodMatcher.CallToSignature[] SERVICE_GET_SIGNATURES = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\DependencyInjection\\ContainerInterface", "get"),
        new MethodMatcher.CallToSignature("\\Psr\\Container\\ContainerInterface", "get"),
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "get"),

        // Symfony 3.3 / 3.4
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\ControllerTrait", "get"),

        // Symfony 4
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController", "get"),

        new MethodMatcher.CallToSignature("\\Symfony\\Component\\DependencyInjection\\ParameterBag\\ContainerBagInterface", "get"),
    };

    private static final Key<CachedValue<Collection<String>>> SYMFONY_CONTAINER_FILES = new Key<>("SYMFONY_CONTAINER_FILES");

    private static final String[] LOWER_PRIORITY = new String[] {
        "debug", "default", "abstract", "inner", "chain", "decorate", "delegat"
    };

    public static final String AUTOWIRE_ATTRIBUTE_CLASS = "\\Symfony\\Component\\DependencyInjection\\Attribute\\Autowire";
    public static final String TAGGED_ITERATOR_ATTRIBUTE_CLASS = "\\Symfony\\Component\\DependencyInjection\\Attribute\\TaggedIterator";
    public static final String TAGGED_LOCATOR_ATTRIBUTE_CLASS = "\\Symfony\\Component\\DependencyInjection\\Attribute\\TaggedLocator";
    public static final String DECORATOR_ATTRIBUTE_CLASS = "\\Symfony\\Component\\DependencyInjection\\Attribute\\AsDecorator";
    public static final String AUTOCONFIGURE_ATTRIBUTE_CLASS = "\\Symfony\\Component\\DependencyInjection\\Attribute\\Autoconfigure";
    public static final String AUTOCONFIGURE_TAG_ATTRIBUTE_CLASS = "\\Symfony\\Component\\DependencyInjection\\Attribute\\AutoconfigureTag";
    public static final String AUTOWIRE_LOCATOR_ATTRIBUTE_CLASS = "\\Symfony\\Component\\DependencyInjection\\Attribute\\AutowireLocator";
    public static final String AUTOWIRE_SERVICE_CLOSURE_ATTRIBUTE_CLASS = "\\Symfony\\Component\\DependencyInjection\\Attribute\\AutowireServiceClosure";
    public static final String AUTOWIRE_CALLABLE_ATTRIBUTE_CLASS = "\\Symfony\\Component\\DependencyInjection\\Attribute\\AutowireCallable";
    public static final String AUTOWIRE_METHOD_OF_ATTRIBUTE_CLASS = "\\Symfony\\Component\\DependencyInjection\\Attribute\\AutowireMethodOf";
    public static final String CONTAINER_CONFIGURATOR = "\\Symfony\\Component\\DependencyInjection\\Loader\\Configurator\\ContainerConfigurator";
    public static final String CONTAINER_CONFIG_APP = "\\Symfony\\Component\\DependencyInjection\\Loader\\Configurator\\App";

    @NotNull
    public static Collection<ServiceSerializable> getServicesInFile(@NotNull PsiFile psiFile) {
        final Collection<ServiceSerializable> services = new ArrayList<>();

        if(psiFile instanceof XmlFile) {
            visitFile((XmlFile) psiFile, serviceConsumer -> {
                SerializableService serializableService = createService(serviceConsumer);
                serializableService.setDecorationInnerName(serviceConsumer.attributes().getString("decoration-inner-name"));
                serializableService.setIsDeprecated(serviceConsumer.attributes().getBoolean("deprecated"));

                services.add(serializableService);
            });
        } else if (psiFile instanceof YAMLFile) {
            visitFile((YAMLFile) psiFile, serviceConsumer -> {

                // alias inline "foo: @bar"
                PsiElement yamlKeyValue = serviceConsumer.attributes().getPsiElement();
                if(yamlKeyValue instanceof YAMLKeyValue) {
                    PsiElement value = ((YAMLKeyValue) yamlKeyValue).getValue();
                    if(value instanceof YAMLScalar) {
                        String valueText = ((YAMLScalar) value).getTextValue();
                        if(StringUtils.isNotBlank(valueText) && valueText.startsWith("@") && valueText.length() > 1) {
                            services.add(
                                new SerializableService(serviceConsumer.getServiceId())
                                    .setAlias(valueText.substring(1))
                                    .setIsAutowire(serviceConsumer.getDefaults().isAutowire())
                                    .setIsPublic(serviceConsumer.getDefaults().isPublic())
                            );
                            return;
                        }
                    }
                }

                SerializableService serializableService = createService(serviceConsumer);
                serializableService.setDecorationInnerName(serviceConsumer.attributes().getString("decoration_inner_name"));

                // catch: deprecated: ~
                String string = serviceConsumer.attributes().getString("deprecated");
                if("~".equals(string)) {
                    serializableService.setIsDeprecated(true);
                } else {
                    serializableService.setIsDeprecated(serviceConsumer.attributes().getBoolean("deprecated"));
                }

                services.add(serializableService);
            });
        } else if (psiFile instanceof PhpFile) {
            visitPhpFluentFile((PhpFile) psiFile, serviceConsumer -> {
                SerializableService serializableService = createService(serviceConsumer);
                serializableService.setDecorationInnerName(serviceConsumer.attributes().getString("decoration_inner_name"));
                serializableService.setIsDeprecated(serviceConsumer.attributes().getBoolean("deprecated"));
                services.add(serializableService);
            });

            services.addAll(getPhpArrayServices((PhpFile) psiFile));
        }

        // decorated services
        services.addAll(getPseudoDecoratedServices(services));

        return services;
    }

    @NotNull
    private static ServiceFileDefaults createDefaults(@NotNull YAMLFile psiFile) {
        YAMLKeyValue yamlKeyValueDefaults = YAMLUtil.getQualifiedKeyInFile(psiFile, "services", "_defaults");

        if(yamlKeyValueDefaults != null) {
            return new ServiceFileDefaults(
                YamlHelper.getYamlKeyValueAsBoolean(yamlKeyValueDefaults, "public"),
                YamlHelper.getYamlKeyValueAsBoolean(yamlKeyValueDefaults, "autowire")
            );
        }

        return ServiceFileDefaults.EMPTY;
    }

    /**
     * "espend.my_next_foo" > "espend.my_next_foo.inner" or custom inner name
     */
    @NotNull
    private static Collection<ServiceSerializable> getPseudoDecoratedServices(@NotNull Collection<ServiceSerializable> services) {
        Collection<ServiceSerializable> decoratedServices = new ArrayList<>();

        for (ServiceSerializable service : services) {
            String decorates = service.getDecorates();
            if(decorates == null || StringUtils.isBlank(decorates)) {
                continue;
            }

            String decorationInnerName = service.getDecorationInnerName();
            if(StringUtils.isBlank(decorationInnerName)) {
                decorationInnerName = service.getId() + ".inner";
            }

            decoratedServices.add(new SerializableService(decorationInnerName));
        }

        return decoratedServices;
    }

    @NotNull
    private static SerializableService createService(@NotNull ServiceConsumer serviceConsumer) {
        AttributeValueInterface attributes = serviceConsumer.attributes();

        Boolean anAbstract = attributes.getBoolean("abstract");
        String aClass = StringUtils.stripStart(attributes.getString("class"), "\\");
        if(aClass == null && isServiceIdAsClassSupported(attributes, anAbstract)) {
            // if no "class" given since Syfmony 3.3 we have lowercase "id" names
            // as we internally use case insensitive maps; add user provided values
            aClass = serviceConsumer.getServiceId();
        }

        return new SerializableService(serviceConsumer.getServiceId())
            .setAlias(attributes.getString("alias"))
            .setClassName(aClass)
            .setDecorates(attributes.getString("decorates"))
            .setParent(attributes.getString("parent"))
            .setIsAbstract(anAbstract)
            .setIsAutowire(attributes.getBoolean("autowire", serviceConsumer.getDefaults().isAutowire()))
            .setIsLazy(attributes.getBoolean("lazy"))
            .setIsPublic(attributes.getBoolean("public", serviceConsumer.getDefaults().isPublic()))
            .setResource(attributes.getStringArray("resource"))
            .setExclude(attributes.getStringArray("exclude"))
            .setTags(attributes.getTags());
    }

    /**
     * Service definition allows "id" to "class" transformation: eg not an alias or abstract service
     */
    private static boolean isServiceIdAsClassSupported(@NotNull AttributeValueInterface attributes, @Nullable Boolean anAbstract) {
        return attributes.getString("alias") == null && !(anAbstract != null && anAbstract);
    }

    public static void visitFile(@NotNull YAMLFile psiFile, @NotNull Consumer<ServiceConsumer> consumer) {
        ServiceFileDefaults defaults = null;

        for (YAMLKeyValue keyValue : YamlHelper.getQualifiedKeyValuesInFile(psiFile, "services")) {
            if(defaults == null) {
                defaults = createDefaults(psiFile);
            }

            String serviceId = keyValue.getKeyText();
            if(StringUtils.isBlank(serviceId) || "_defaults".equals(serviceId)) {
                continue;
            }

            consumer.consume(new ServiceConsumer(keyValue, serviceId, new YamlKeyValueAttributeValue(keyValue), defaults));
        }
    }

    public static void visitFile(@NotNull XmlFile psiFile, @NotNull Consumer<ServiceConsumer> consumer) {
        if(!(psiFile.getFirstChild() instanceof XmlDocument)) {
            return;
        }

        XmlTag[] xmlTags = PsiTreeUtil.getChildrenOfType(psiFile.getFirstChild(), XmlTag.class);
        if(xmlTags == null) {
            return;
        }

        for(XmlTag xmlTag: xmlTags) {
            if(xmlTag.getName().equals("container")) {
                for(XmlTag servicesTag: xmlTag.getSubTags()) {
                    if("services".equals(servicesTag.getName())) {
                        // default values:
                        // <defaults autowire="true" public="false" />
                        ServiceFileDefaults defaults = createDefaults(servicesTag);

                        for (XmlTag serviceTag: servicesTag.findSubTags("service")) {
                            String serviceId = serviceTag.getAttributeValue("id");
                            if (StringUtils.isBlank(serviceId)) {
                                continue;
                            }

                            consumer.consume(new ServiceConsumer(serviceTag, serviceId, new XmlTagAttributeValue(serviceTag), defaults));
                        }

                        // <prototype namespace="App\"
                        //    resource="../src/*"
                        //    exclude="../src/{DependencyInjection,Entity,Migrations,Tests,Kernel.php}"/>
                        for (XmlTag serviceTag: servicesTag.findSubTags("prototype")) {
                            String namespace = serviceTag.getAttributeValue("namespace");
                            if (StringUtils.isBlank(namespace)) {
                                continue;
                            }

                            consumer.consume(new ServiceConsumer(serviceTag, namespace, new XmlTagAttributeValue(serviceTag), defaults));
                        }
                    }
                }
            }
        }
    }

    @Nullable
    private static String getStringValueIndexSafe(@Nullable PsiElement psiElement) {
        if (psiElement == null) {
            return null;
        }

        String serviceClass = null;
        if (psiElement instanceof StringLiteralExpression) {
            serviceClass = normalizePhpStringValue(((StringLiteralExpression) psiElement).getContents());
        } else if(psiElement instanceof ClassConstantReference) {
            serviceClass = PhpElementsUtil.getClassConstantPhpFqn((ClassConstantReference) psiElement);
        }

        return StringUtils.isNotBlank(serviceClass)
            ? serviceClass
            : null;
    }

    /**
     * Extract class parameter from chained ->class() method call
     *
     * $services->set('id')->class(Foo::class)->tag('foo')
     */
    @Nullable
    private static String getChainedClassMethodParameter(@NotNull MethodReference setMethodReference) {
        // Walk up the tree to find chained method calls
        // Parents of MethodReference in a chain are always MethodReference, so we can break early
        // Also break when we find another 'set' method (different service)
        PsiElement parent = setMethodReference.getParent();
        while (parent instanceof MethodReference methodRef) {
            String methodName = methodRef.getName();

            // Stop if we encounter another 'set' method (different service definition)
            if ("set".equals(methodName)) {
                break;
            }

            if ("class".equals(methodName)) {
                // Check if this ->class() is called on our ->set() method
                PhpExpression classReference = methodRef.getClassReference();
                if (classReference instanceof MethodReference && PsiTreeUtil.isAncestor(setMethodReference, classReference, false)) {
                    // Extract the first parameter from ->class(Foo::class)
                    PsiElement[] parameters = methodRef.getParameters();
                    if (parameters.length >= 1) {
                        return getStringValueIndexSafe(parameters[0]);
                    }
                }
            }
            parent = parent.getParent();
        }
        return null;
    }

    /**
     * Finds methods / functions that have a valid ContainerConfigurator parameter for services
     *
     * Supported:
     *  - public function loadExtension(array $config, ContainerConfigurator $container, ContainerBuilder $builder): void
     *  - return static function (ContainerConfigurator $container) { ... }
     */
    @NotNull
    private static Collection<Function> getPhpContainerConfiguratorFunctions(@NotNull PhpFile phpFile) {
        Collection<Function> functions = new HashSet<>();


        for (PhpNamespace phpNamespace : PhpNamespaceBraceConverter.getAllNamespaces(phpFile)) {
            // its used for all service files:
            // namespace \Symfony\Component\DependencyInjection\Loader\Configurator { ... }
            String fqn = phpNamespace.getFQN();
            if (!fqn.equals("\\Symfony\\Component\\DependencyInjection\\Loader\\Configurator")) {
                continue;
            }

            Collection<PhpReturn> phpReturns = new ArrayList<>();

            PhpControlFlowUtil.processFlow(phpNamespace.getControlFlow(), new PhpInstructionProcessor() {
                @Override
                public boolean processReturnInstruction(PhpReturnInstruction instruction) {
                    PsiElement argument = instruction.getArgument();
                    if (argument != null && argument.getParent() instanceof PhpReturn phpReturn) {
                        phpReturns.add(phpReturn);
                    }

                    return super.processReturnInstruction(instruction);
                }
            });

            for (PhpReturn phpReturn : phpReturns) {
                for (Function function : PsiTreeUtil.collectElementsOfType(phpReturn, Function.class)) {
                    Parameter parameter = function.getParameter(0);
                    if (parameter == null) {
                        continue;
                    }

                    // \Symfony\Component\DependencyInjection\Loader\Configurator\ContainerConfigurator
                    Set<@NlsSafe String> types = parameter.getLocalType().getTypes();
                    if (types.stream().noneMatch(s -> s.contains(CONTAINER_CONFIGURATOR))) {
                        continue;
                    }

                    functions.add(function);
                }
            }
        }

        // Also check for methods in classes that have a ContainerConfigurator parameter
        // Example: Bundle::loadExtension(array $config, ContainerConfigurator $container, ...)
        for (PhpClass phpClass : PhpPsiUtil.findAllClasses(phpFile)) {
            for (Method method : phpClass.getOwnMethods()) {
                if (!"loadExtension".equals(method.getName())) {
                    continue;
                }

                // ContainerConfigurator is always the second parameter (index 1) in loadExtension
                Parameter parameter = method.getParameter(1);
                if (parameter == null) {
                    continue;
                }

                Set<@NlsSafe String> types = parameter.getLocalType().getTypes();
                if (types.stream().anyMatch(s -> s.contains(CONTAINER_CONFIGURATOR))) {
                    functions.add(method);
                }
            }
        }

        return functions;
    }

    /**
     * Services as PHP definition
     *
     * "namespace Symfony\Component\DependencyInjection\Loader\Configurator;"
     * "..."
     * "return static function (ContainerConfigurator $container) { ... }"
     */
    public static void visitFile(@NotNull PhpFile phpFile, @NotNull Consumer<ServiceConsumer> consumer) {
        visitPhpFluentFile(phpFile, consumer);
        visitPhpArrayConfig(phpFile, consumer);
    }

    private static void visitPhpFluentFile(@NotNull PhpFile phpFile, @NotNull Consumer<ServiceConsumer> consumer) {
        for (Function function : getPhpContainerConfiguratorFunctions(phpFile)) {
            ServiceFileDefaults defaults = createFluentDefaults(function);

            // we only want "set" and "alias" methods
            for (MethodReference methodReference : PhpElementsUtil.collectMethodReferencesInsideControlFlow(function, "set", "alias")) {
                // ->set('translator.default', Translator::class)
                if ("set".equals(methodReference.getName())) {
                    // Skip ->set() calls on ->parameters() — those are container parameters, not services
                    if (isFluentParametersSet(methodReference)) {
                        continue;
                    }

                    PsiElement[] parameters = methodReference.getParameters();
                    String serviceName = null;
                    if (parameters.length >= 1) {
                        serviceName = getStringValueIndexSafe(parameters[0]);
                    }

                    if (StringUtils.isBlank(serviceName)) {
                        continue;
                    }

                    Map<String, String> keyValue = new HashMap<>();
                    if (parameters.length >= 2) {
                        String serviceClass = getStringValueIndexSafe(parameters[1]);
                        if (StringUtils.isNotBlank(serviceClass)) {
                            keyValue.put("class", serviceClass);
                        }
                    }

                    // Check for chained ->class() method call
                    // $services->set('id')->class(Foo::class)
                    if (!keyValue.containsKey("class")) {
                        String chainedClass = getChainedClassMethodParameter(methodReference);
                        if (StringUtils.isNotBlank(chainedClass)) {
                            keyValue.put("class", chainedClass);
                        }
                    }

                    List<String> tags = new ArrayList<>();
                    Map<String, Collection<String>> arrayValues = new HashMap<>();
                    collectFluentChainAttributes(methodReference, keyValue, tags, arrayValues);

                    consumer.consume(new ServiceConsumer(
                        parameters[0],
                        serviceName,
                        new PhpKeyValueAttributeValue(methodReference, keyValue, arrayValues, tags),
                        defaults
                    ));
                }

                // ->alias(TranslatorInterface::class, 'translator')
                if ("alias".equals(methodReference.getName())) {
                    PsiElement[] parameters = methodReference.getParameters();
                    String serviceName = null;
                    if (parameters.length >= 1) {
                        serviceName = getStringValueIndexSafe(parameters[0]);
                    }

                    if (StringUtils.isBlank(serviceName)) {
                        continue;
                    }

                    Map<String, String> keyValue = new HashMap<>();
                    if (parameters.length >= 2) {
                        String serviceClass = getStringValueIndexSafe(parameters[1]);
                        if (StringUtils.isNotBlank(serviceClass)) {
                            keyValue.put("alias", serviceClass);
                        }
                    }

                    consumer.consume(new ServiceConsumer(parameters[0], serviceName, new PhpKeyValueAttributeValue(methodReference, keyValue), defaults));
                }
            }
        }
    }

    /**
     * Returns true if this ->set() call is on the parameters configurator chain
     * ($container->parameters()->set(...)) rather than the services chain.
     *
     * Walks the classRef chain downward from ->set() looking for ->parameters()
     * before ->services(). This filters out container parameter definitions that
     * would otherwise be misidentified as service definitions.
     */
    private static boolean isFluentParametersSet(@NotNull MethodReference setMethodReference) {
        return "parameters".equals(getFluentConfiguratorType(setMethodReference.getClassReference(), new HashSet<>()));
    }

    /**
     * Resolve the fluent configurator kind behind a method chain or local variable.
     *
     * Supports direct chains like "$container->parameters()->set(...)" and variable-backed
     * chains like "$parameters = $container->parameters(); $parameters->set(...)".
     *
     * Returns "parameters", "services", or null when the origin cannot be determined.
     */
    @Nullable
    static String getFluentConfiguratorType(@Nullable PsiElement psiElement, @NotNull Set<PsiElement> visited) {
        // Guard against incomplete PSI and cyclic variable resolution.
        if (psiElement == null || !visited.add(psiElement)) {
            return null;
        }

        if (psiElement instanceof MethodReference methodRef) {
            String name = methodRef.getName();
            if ("parameters".equals(name) || "services".equals(name)) {
                return name;
            }

            return getFluentConfiguratorType(methodRef.getClassReference(), visited);
        }

        if (psiElement instanceof Variable variable) {
            // Indexers must stay syntax-only here: resolving variables can hit stub/type indexes.
            PsiElement assignedValue = getPreviousVariableAssignmentValue(variable);
            if (assignedValue != null) {
                return getFluentConfiguratorType(assignedValue, visited);
            }
        }

        return null;
    }

    @Nullable
    private static PsiElement getPreviousVariableAssignmentValue(@NotNull Variable variable) {
        String variableName = variable.getName();
        if (StringUtils.isBlank(variableName)) {
            return null;
        }

        Function scope = PsiTreeUtil.getParentOfType(variable, Function.class);
        if (scope == null) {
            return null;
        }

        AssignmentExpression latestAssignment = null;
        int variableOffset = variable.getTextOffset();

        for (AssignmentExpression assignmentExpression : PsiTreeUtil.collectElementsOfType(scope, AssignmentExpression.class)) {
            if (assignmentExpression.getTextOffset() >= variableOffset) {
                continue;
            }

            if (!(assignmentExpression.getVariable() instanceof Variable assignedVariable) || !variableName.equals(assignedVariable.getName())) {
                continue;
            }

            latestAssignment = assignmentExpression;
        }

        return latestAssignment != null ? latestAssignment.getValue() : null;
    }

    /**
     * Parse ->defaults()->autowire()->public() from the ContainerConfigurator function
     * to produce a ServiceFileDefaults for the whole file scope.
     */
    @NotNull
    private static ServiceFileDefaults createFluentDefaults(@NotNull Function function) {
        for (MethodReference defaultsRef : PhpElementsUtil.collectMethodReferencesInsideControlFlow(function, "defaults")) {
            Boolean isPublic = null;
            Boolean isAutowire = null;

            PsiElement parent = defaultsRef.getParent();
            while (parent instanceof MethodReference chainedMethod) {
                PsiElement[] params = chainedMethod.getParameters();
                switch (StringUtils.defaultString(chainedMethod.getName())) {
                    case "autowire" -> isAutowire = extractFluentBooleanParam(params, true);
                    case "public" -> isPublic = extractFluentBooleanParam(params, true);
                    case "private" -> isPublic = false;
                }
                parent = parent.getParent();
            }

            return new ServiceFileDefaults(isPublic, isAutowire);
        }

        return ServiceFileDefaults.EMPTY;
    }

    /**
     * Extract a boolean from a no-arg or single PHP literal arg method call.
     *
     * ->autowire()      → defaultWhenEmpty (true)
     * ->autowire(true)  → true
     * ->autowire(false) → false
     */
    @NotNull
    private static Boolean extractFluentBooleanParam(@NotNull PsiElement[] params, boolean defaultWhenEmpty) {
        if (params.length == 0) {
            return defaultWhenEmpty;
        }

        if (params[0] instanceof ConstantReference constantRef) {
            String name = constantRef.getName();
            if ("false".equalsIgnoreCase(name)) return false;
            if ("true".equalsIgnoreCase(name)) return true;
        }

        return defaultWhenEmpty;
    }

    /**
     * Walk up the PSI method chain from ->set() and collect all attributes contributed
     * by chained calls: ->tag(), ->decorate(), ->parent(), ->public(), ->lazy(), etc.
     *
     * In PSI, $services->set('id')->tag('foo')->public() is represented as nested
     * MethodReferences where each outer call wraps the inner. Walking getParent()
     * upward from ->set() visits each subsequent method in chain order.
     */
    private static void collectFluentChainAttributes(
        @NotNull MethodReference setMethodReference,
        @NotNull Map<String, String> keyValue,
        @NotNull List<String> tags,
        @NotNull Map<String, Collection<String>> arrayValues
    ) {
        List<String> resource = new ArrayList<>();
        List<String> exclude = new ArrayList<>();

        PsiElement parent = setMethodReference.getParent();
        while (parent instanceof MethodReference chainedMethod) {
            String methodName = chainedMethod.getName();

            // Stop when we reach another service definition
            if ("set".equals(methodName) || "alias".equals(methodName)) {
                break;
            }

            PsiElement[] params = chainedMethod.getParameters();

            switch (StringUtils.defaultString(methodName)) {
                case "tag" -> {
                    // ->tag('name') or ->tag('name', ['attr' => 'value'])
                    if (params.length >= 1) {
                        String tagName = getStringValueIndexSafe(params[0]);
                        if (StringUtils.isNotBlank(tagName)) {
                            tags.add(tagName);
                        }
                    }
                }
                case "decorate" -> {
                    // ->decorate('service.id') or ->decorate('service.id', 'inner.name')
                    if (params.length >= 1) {
                        String decorates = getStringValueIndexSafe(params[0]);
                        if (StringUtils.isNotBlank(decorates)) {
                            keyValue.put("decorates", decorates);
                        }
                    }
                    if (params.length >= 2) {
                        String innerName = getStringValueIndexSafe(params[1]);
                        if (StringUtils.isNotBlank(innerName)) {
                            keyValue.put("decoration_inner_name", innerName);
                        }
                    }
                }
                case "parent" -> {
                    if (params.length >= 1) {
                        String parentService = getStringValueIndexSafe(params[0]);
                        if (StringUtils.isNotBlank(parentService)) {
                            keyValue.put("parent", parentService);
                        }
                    }
                }
                case "public" -> keyValue.put("public", String.valueOf(extractFluentBooleanParam(params, true)));
                case "private" -> keyValue.put("public", "false");
                case "autowire" -> keyValue.put("autowire", String.valueOf(extractFluentBooleanParam(params, true)));
                case "lazy" -> keyValue.put("lazy", "true");
                case "abstract" -> keyValue.put("abstract", "true");
                case "deprecated" -> keyValue.put("deprecated", "true");
                case "load" -> {
                    // not supported
                }
                case "exclude" -> {
                    if (params.length >= 1) {
                        String excludePath = getStringValueIndexSafe(params[0]);
                        if (StringUtils.isNotBlank(excludePath)) {
                            exclude.add(excludePath);
                        }
                    }
                }
            }

            parent = parent.getParent();
        }

        if (!resource.isEmpty()) {
            arrayValues.put("resource", resource);
        }
        if (!exclude.isEmpty()) {
            arrayValues.put("exclude", exclude);
        }
    }

    private static void visitPhpArrayConfig(@NotNull PhpFile phpFile, @NotNull Consumer<ServiceConsumer> consumer) {
        for (PsiElement psiElement : collectPhpConfigReturnArguments(phpFile)) {
            ArrayCreationExpression configArray = extractReturnedConfigArray(psiElement);
            if (configArray == null) {
                continue;
            }

            visitPhpArrayServices(configArray, createPhpArrayDefaults(configArray), consumer);
        }
    }

    @NotNull
    private static Collection<ServiceSerializable> getPhpArrayServices(@NotNull PhpFile phpFile) {
        Collection<ServiceSerializable> services = new ArrayList<>();

        for (PsiElement psiElement : collectPhpConfigReturnArguments(phpFile)) {
            ArrayCreationExpression configArray = extractReturnedConfigArray(psiElement);
            if (configArray == null) {
                continue;
            }

            services.addAll(createServicesFromPhpArray(configArray, createPhpArrayDefaults(configArray)));
        }

        return services;
    }

    @NotNull
    private static Collection<PsiElement> collectPhpConfigReturnArguments(@NotNull PhpFile phpFile) {
        Collection<PsiElement> elements = new LinkedHashSet<>();

        collectTopLevelReturnArguments(phpFile, phpFile, elements);

        for (PhpNamespace phpNamespace : PhpNamespaceBraceConverter.getAllNamespaces(phpFile)) {
            collectTopLevelReturnArguments(phpNamespace, phpNamespace, elements);
        }

        for (Function function : getPhpContainerConfiguratorFunctions(phpFile)) {
            elements.addAll(PhpElementsUtil.collectPhpReturnArgumentsInsideControlFlow(function));
        }

        return elements;
    }

    private static void collectTopLevelReturnArguments(@NotNull PhpScopeHolder phpScopeHolder, @NotNull PsiElement boundary, @NotNull Collection<PsiElement> elements) {
        for (PsiElement argument : PhpElementsUtil.collectPhpReturnArgumentsInsideControlFlow(phpScopeHolder)) {
            PsiElement parentScope = PsiTreeUtil.getParentOfType(argument, Function.class, Method.class, PhpClass.class);
            if (parentScope != null && PsiTreeUtil.isAncestor(boundary, parentScope, false)) {
                continue;
            }

            elements.add(argument);
        }
    }

    @Nullable
    private static ArrayCreationExpression extractReturnedConfigArray(@NotNull PsiElement psiElement) {
        if (psiElement instanceof ArrayCreationExpression arrayCreationExpression) {
            return arrayCreationExpression;
        }

        if (!(psiElement instanceof MethodReference methodReference) || !isConfigFactoryCall(methodReference)) {
            return null;
        }

        PsiElement[] parameters = methodReference.getParameters();
        if (parameters.length == 0 || !(parameters[0] instanceof ArrayCreationExpression arrayCreationExpression)) {
            return null;
        }

        return arrayCreationExpression;
    }

    private static boolean isConfigFactoryCall(@NotNull MethodReference methodReference) {
        if (!"config".equals(methodReference.getName())) {
            return false;
        }

        PhpExpression classReference = methodReference.getClassReference();
        if (classReference == null) {
            return false;
        }

        PsiElement resolved = classReference.getReference() != null ? classReference.getReference().resolve() : null;
        if (resolved instanceof PhpClass phpClass) {
            return CONTAINER_CONFIG_APP.equals(phpClass.getFQN());
        }

        String text = StringUtils.stripStart(classReference.getText(), "\\");
        return "App".equals(text) || CONTAINER_CONFIG_APP.substring(1).equals(text);
    }

    @Nullable
    private static ArrayCreationExpression getPhpArrayServicesValue(@NotNull ArrayCreationExpression configArray) {
        PhpPsiElement services = PhpElementsUtil.getArrayValue(configArray, "services");
        return services instanceof ArrayCreationExpression ? (ArrayCreationExpression) services : null;
    }

    /**
     * Extract `_defaults` for a single PHP array config root.
     */
    @NotNull
    private static ServiceFileDefaults createPhpArrayDefaults(@NotNull ArrayCreationExpression configArray) {
        ArrayCreationExpression services = getPhpArrayServicesValue(configArray);
        if (services == null) {
            return ServiceFileDefaults.EMPTY;
        }

        PhpPsiElement defaults = PhpElementsUtil.getArrayValue(services, "_defaults");
        if (!(defaults instanceof ArrayCreationExpression defaultsArray)) {
            return ServiceFileDefaults.EMPTY;
        }

        return new ServiceFileDefaults(
            PhpElementsUtil.getArrayValueBool(defaultsArray, "public"),
            PhpElementsUtil.getArrayValueBool(defaultsArray, "autowire")
        );
    }

    private static void visitPhpArrayServices(@NotNull ArrayCreationExpression configArray, @NotNull ServiceFileDefaults defaults, @NotNull Consumer<ServiceConsumer> consumer) {
        ArrayCreationExpression services = getPhpArrayServicesValue(configArray);
        if (services == null) {
            return;
        }

        for (ArrayHashElement hashElement : services.getHashElements()) {
            String serviceId = getPhpArrayKey(hashElement.getKey());
            if (StringUtils.isBlank(serviceId) || "_defaults".equals(serviceId)) {
                continue;
            }

            consumer.consume(new ServiceConsumer(
                hashElement.getKey(),
                serviceId,
                new PhpKeyValueAttributeValue(
                    hashElement,
                    createPhpArrayServiceAttributeMap(hashElement.getValue()),
                    createPhpArrayServiceStringArrayMap(hashElement.getValue()),
                    getPhpArrayTags(hashElement.getValue() instanceof ArrayCreationExpression arrayCreationExpression ? PhpElementsUtil.getArrayValue(arrayCreationExpression, "tags") : null)
                ),
                defaults
            ));
        }
    }

    @NotNull
    private static Collection<ServiceSerializable> createServicesFromPhpArray(@NotNull ArrayCreationExpression configArray, @NotNull ServiceFileDefaults defaults) {
        Collection<ServiceSerializable> services = new ArrayList<>();

        ArrayCreationExpression phpServices = getPhpArrayServicesValue(configArray);
        if (phpServices == null) {
            return services;
        }

        for (ArrayHashElement hashElement : phpServices.getHashElements()) {
            String serviceId = getPhpArrayKey(hashElement.getKey());
            if (StringUtils.isBlank(serviceId) || "_defaults".equals(serviceId)) {
                continue;
            }

            PsiElement value = hashElement.getValue();
            SerializableService serializableService = new SerializableService(serviceId)
                .setIsAutowire(defaults.isAutowire())
                .setIsPublic(defaults.isPublic());

            if (value instanceof ArrayCreationExpression arrayCreationExpression) {
                Boolean isAbstract = PhpElementsUtil.getArrayValueBool(arrayCreationExpression, "abstract");
                Boolean isAutowire = PhpElementsUtil.getArrayValueBool(arrayCreationExpression, "autowire");
                Boolean isPublic = PhpElementsUtil.getArrayValueBool(arrayCreationExpression, "public");
                String className = StringUtils.stripStart(getPhpArrayValueString(arrayCreationExpression, "class"), "\\");
                if (className == null && isPhpArrayServiceIdAsClassSupported(arrayCreationExpression, isAbstract)) {
                    className = serviceId;
                }

                Collection<String> tags = getPhpArrayTags(PhpElementsUtil.getArrayValue(arrayCreationExpression, "tags"));

                services.add(serializableService
                    .setClassName(className)
                    .setAlias(getPhpArrayValueString(arrayCreationExpression, "alias"))
                    .setDecorates(getPhpArrayValueString(arrayCreationExpression, "decorates"))
                    .setDecorationInnerName(getFirstNotBlank(
                        getPhpArrayValueString(arrayCreationExpression, "decoration_inner_name"),
                        getPhpArrayValueString(arrayCreationExpression, "decoration-inner-name")
                    ))
                    .setParent(getPhpArrayValueString(arrayCreationExpression, "parent"))
                    .setIsAbstract(isAbstract)
                    .setIsAutowire(isAutowire != null ? isAutowire : defaults.isAutowire())
                    .setIsLazy(PhpElementsUtil.getArrayValueBool(arrayCreationExpression, "lazy"))
                    .setIsPublic(isPublic != null ? isPublic : defaults.isPublic())
                    .setIsDeprecated(PhpElementsUtil.getArrayValueBool(arrayCreationExpression, "deprecated"))
                    .setResource(getPhpArrayStringValues(PhpElementsUtil.getArrayValue(arrayCreationExpression, "resource")))
                    .setExclude(getPhpArrayStringValues(PhpElementsUtil.getArrayValue(arrayCreationExpression, "exclude")))
                    .setTags(tags)
                );
                continue;
            }

            String scalarValue = getStringValueIndexSafe(value);
            if (StringUtils.isNotBlank(scalarValue) && scalarValue.startsWith("@") && scalarValue.length() > 1) {
                services.add(serializableService.setAlias(scalarValue.substring(1)));
                continue;
            }

            if (StringUtils.isNotBlank(scalarValue)) {
                services.add(serializableService.setClassName(StringUtils.stripStart(scalarValue, "\\")));
                continue;
            }

            services.add(serializableService.setClassName(serviceId));
        }

        return services;
    }

    @NotNull
    private static Map<String, String> createPhpArrayServiceAttributeMap(@Nullable PsiElement value) {
        if (value instanceof ArrayCreationExpression arrayCreationExpression) {
            return createPhpArrayServiceAttributeValueMap(arrayCreationExpression);
        }

        String scalarValue = getStringValueIndexSafe(value);
        if (StringUtils.isBlank(scalarValue)) {
            return Collections.emptyMap();
        }

        Map<String, String> values = new HashMap<>();
        if (scalarValue.startsWith("@") && scalarValue.length() > 1) {
            values.put("alias", scalarValue.substring(1));
        } else {
            values.put("class", StringUtils.stripStart(scalarValue, "\\"));
        }

        return values;
    }

    @NotNull
    private static Map<String, Collection<String>> createPhpArrayServiceStringArrayMap(@Nullable PsiElement value) {
        if (!(value instanceof ArrayCreationExpression arrayCreationExpression)) {
            return Collections.emptyMap();
        }

        Map<String, Collection<String>> values = new HashMap<>();
        values.put("resource", getPhpArrayStringValues(PhpElementsUtil.getArrayValue(arrayCreationExpression, "resource")));
        values.put("exclude", getPhpArrayStringValues(PhpElementsUtil.getArrayValue(arrayCreationExpression, "exclude")));

        return values;
    }

    @NotNull
    private static Map<String, String> createPhpArrayServiceAttributeValueMap(@NotNull ArrayCreationExpression arrayCreationExpression) {
        Map<String, String> values = new HashMap<>();

        putPhpArrayStringValue(values, "class", arrayCreationExpression, "class");
        putPhpArrayStringValue(values, "alias", arrayCreationExpression, "alias");
        putPhpArrayStringValue(values, "decorates", arrayCreationExpression, "decorates");
        putPhpArrayStringValue(values, "decoration_inner_name", arrayCreationExpression, "decoration_inner_name");
        putPhpArrayStringValue(values, "decoration_inner_name", arrayCreationExpression, "decoration-inner-name");
        putPhpArrayStringValue(values, "parent", arrayCreationExpression, "parent");

        return values;
    }

    private static boolean isPhpArrayServiceIdAsClassSupported(@NotNull ArrayCreationExpression arrayCreationExpression, @Nullable Boolean anAbstract) {
        return getPhpArrayValueString(arrayCreationExpression, "alias") == null && !(anAbstract != null && anAbstract);
    }

    private static void putPhpArrayStringValue(@NotNull Map<String, String> values, @NotNull String targetKey, @NotNull ArrayCreationExpression arrayCreationExpression, @NotNull String sourceKey) {
        String value = getPhpArrayValueString(arrayCreationExpression, sourceKey);
        if (StringUtils.isNotBlank(value)) {
            values.put(targetKey, StringUtils.stripStart(value, "\\"));
        }
    }

    @Nullable
    private static String getPhpArrayValueString(@NotNull ArrayCreationExpression arrayCreationExpression, @NotNull String key) {
        PhpPsiElement value = PhpElementsUtil.getArrayValue(arrayCreationExpression, key);
        return value != null ? normalizePhpStringValue(getStringValueIndexSafe(value)) : null;
    }

    @Nullable
    private static String getPhpArrayKey(@Nullable PsiElement psiElement) {
        if (psiElement == null) {
            return null;
        }

        return StringUtils.stripStart(getStringValueIndexSafe(psiElement), "\\");
    }

    @NotNull
    private static Collection<String> getPhpArrayStringValues(@Nullable PsiElement psiElement) {
        if (psiElement instanceof ArrayCreationExpression arrayCreationExpression) {
            Collection<String> values = new HashSet<>();
            for (PsiElement value : PhpElementsUtil.getArrayValues(arrayCreationExpression)) {
                String stringValue = getStringValueIndexSafe(value);
                if (StringUtils.isNotBlank(stringValue)) {
                    values.add(stringValue);
                }
            }
            return values;
        }

        String value = getStringValueIndexSafe(psiElement);
        if (StringUtils.isBlank(value)) {
            return new HashSet<>();
        }

        return new HashSet<>(Collections.singletonList(value));
    }

    @NotNull
    private static Collection<String> getPhpArrayTags(@Nullable PsiElement psiElement) {
        Collection<String> tags = new HashSet<>();

        if (!(psiElement instanceof ArrayCreationExpression arrayCreationExpression)) {
            return tags;
        }

        for (PsiElement tagElement : PhpElementsUtil.getArrayValues(arrayCreationExpression)) {
            if (tagElement instanceof ArrayCreationExpression tagConfig) {
                String name = getPhpArrayValueString(tagConfig, "name");
                if (StringUtils.isNotBlank(name)) {
                    tags.add(name);
                }
                continue;
            }

            String tag = getStringValueIndexSafe(tagElement);
            if (StringUtils.isNotBlank(tag)) {
                tags.add(tag);
            }
        }

        return tags;
    }

    @Nullable
    private static String getFirstNotBlank(@Nullable String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }

        return null;
    }

    @Nullable
    public static String normalizePhpStringValue(@Nullable String value) {
        return value != null ? value.replace("\\\\", "\\") : null;
    }

    /**
     * Extract default values for services tag scope
     *
     * <defaults autowire="true" public="false" />
     */
    private static ServiceFileDefaults createDefaults(@NotNull XmlTag servicesTag) {
        XmlTag xmlDefaults = servicesTag.findFirstSubTag("defaults");
        if(xmlDefaults == null) {
            return ServiceFileDefaults.EMPTY;
        }

        return new ServiceFileDefaults(
            getBooleanValueOf(xmlDefaults.getAttributeValue("public")),
            getBooleanValueOf(xmlDefaults.getAttributeValue("autowire"))
        );
    }

    /**
     * Provide custom "Boolean.valueOf" with nullable support
     */
    @Nullable
    private static Boolean getBooleanValueOf(@Nullable String value) {
        if(value == null) {
            return null;
        }

        return switch (value.toLowerCase()) {
            case "false" -> false;
            case "true" -> true;
            default -> null;
        };

    }

    /**
     * foo:
     *  class: Foo
     *  arguments: [@<caret>]
     *  arguments:
     *      - @<caret>
     */
    @Nullable
    public static ServiceTypeHint getYamlConstructorTypeHint(@NotNull PsiElement psiElement, @NotNull ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {
        if (!YamlHelper.isStringValue(psiElement)) {
            return null;
        }

        // @TODO: simplify code checks
        PsiElement yamlScalar = psiElement.getContext();
        if(!(yamlScalar instanceof YAMLScalar)) {
            return null;
        }

        return getYamlConstructorTypeHint((YAMLScalar) yamlScalar, lazyServiceCollector);
    }

    /**
     * Resolve class for attribute and its parameter index
     *
     * class OurClass {
     *   public function __construct(#[Autowire(service: 'some_service')] private $service1) {}
     *   public function setFoo(#[Autowire(service: 'some_service')] private $service1) {}
     *  }
     */
    public static ServiceTypeHint getPhpAttributeConstructorTypeHint(@NotNull PsiElement psiElement) {
        if (!(psiElement instanceof StringLiteralExpression)) {
            return null;
        }

        PsiElement parameterList = psiElement.getContext();
        if (!(parameterList instanceof ParameterList)) {
            return null;
        }

        PsiElement colon = PsiTreeUtil.prevCodeLeaf(psiElement);
        if (colon == null || colon.getNode().getElementType() != PhpTokenTypes.opCOLON) {
            return null;
        }

        PsiElement argumentName = PsiTreeUtil.prevCodeLeaf(colon);
        if (argumentName == null || argumentName.getNode().getElementType() != PhpTokenTypes.IDENTIFIER) {
            return null;
        }

        // now resolve method attribute: its class and parameter index
        PsiElement phpAttribute = parameterList.getParent();
        if (phpAttribute instanceof PhpAttribute) {
            PsiElement phpAttributesList = phpAttribute.getParent();
            if (phpAttributesList instanceof PhpAttributesList)  {
                PsiElement parameter = phpAttributesList.getParent();
                if (parameter instanceof Parameter) {
                    PsiElement parameterListMethod = parameter.getParent();
                    if (parameterListMethod instanceof ParameterList) {
                        PsiElement method = parameterListMethod.getParent();
                        if (method instanceof Method) {
                            ParameterBag currentParameterIndex = PsiElementUtils.getCurrentParameterIndex((Parameter) parameter);
                            if (currentParameterIndex != null) {
                                return new ServiceTypeHint((Method) method, currentParameterIndex.getIndex(), psiElement);
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * PHP array service config constructor argument type hint:
     *
     * 'Foo\Bar\Car' => [
     *     'arguments' => [service('<caret>')]
     *     'arguments' => ['$apple' => service('<caret>')]
     *     'arguments' => ['@<caret>']
     * ]
     */
    @Nullable
    public static ServiceTypeHint getPhpArrayConstructorTypeHint(
        @NotNull PsiElement psiElement,
        @NotNull ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector
    ) {
        PsiElement element = psiElement instanceof StringLiteralExpression
            ? psiElement
            : psiElement.getParent() instanceof StringLiteralExpression ? psiElement.getParent() : null;
        if (element == null) {
            return null;
        }

        // Determine the element to pass to isInsidePhpArrayServiceConfig / getKeyPath.
        // For service('...') / ref('...') calls the StringLiteralExpression is nested inside a FunctionReference
        // which is the direct array value. For raw '@foo' strings the element itself is the direct array value.
        FunctionReference funcRef = PsiTreeUtil.getParentOfType(element, FunctionReference.class);
        String funcName = funcRef != null ? funcRef.getName() : null;
        boolean isServiceCall = "service".equals(funcName) || "ref".equals(funcName);
        PsiElement lookupElement = isServiceCall ? funcRef : element;

        if (!PhpArrayServiceUtil.isInsidePhpArrayServiceConfig(lookupElement)) {
            return null;
        }

        PhpArrayServiceUtil.ServiceConfigPath keyPath = PhpArrayServiceUtil.getKeyPath(lookupElement);
        if (keyPath == null) {
            return null;
        }

        String[] segments = keyPath.segments();
        if (segments.length < 2 || !"arguments".equals(segments[0])) {
            return null;
        }

        String serviceClass = PhpArrayServiceUtil.getCurrentServiceClass(lookupElement);
        if (StringUtils.isBlank(serviceClass)) {
            return null;
        }

        PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(lookupElement.getProject(), serviceClass, lazyServiceCollector);
        if (phpClass == null) {
            return null;
        }

        Method constructor = phpClass.getConstructor();
        if (constructor == null) {
            return null;
        }

        int parameterIndex = getPhpArrayConstructorArgumentIndex(segments[1], constructor);
        if (parameterIndex < 0) {
            return null;
        }

        return new ServiceTypeHint(constructor, parameterIndex, element);
    }

    private static int getPhpArrayConstructorArgumentIndex(@NotNull String segment, @NotNull Method constructor) {
        if (segment.startsWith("$") && segment.length() > 1) {
            return PhpElementsUtil.getFunctionArgumentByName(constructor, StringUtils.stripStart(segment, "$"));
        }

        try {
            return Integer.parseInt(segment);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * PHP fluent service config constructor argument type hint:
     *
     * $container->services()
     *     ->set('foo', Foo\Bar\Car::class)
     *     ->args([service('<caret>')])
     */
    @Nullable
    public static ServiceTypeHint getPhpFluentConstructorTypeHint(
        @NotNull PsiElement psiElement,
        @NotNull ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector
    ) {
        PsiElement element = psiElement instanceof StringLiteralExpression
            ? psiElement
            : psiElement.getParent() instanceof StringLiteralExpression ? psiElement.getParent() : null;
        if (element == null) {
            return null;
        }

        // Must be inside service() or ref() call
        PsiElement functionParameterList = element.getParent();
        if (!(functionParameterList instanceof ParameterList)) {
            return null;
        }

        PsiElement functionReference = functionParameterList.getParent();
        if (!(functionReference instanceof FunctionReference)) {
            return null;
        }

        String functionName = ((FunctionReference) functionReference).getName();
        if (!"service".equals(functionName) && !"ref".equals(functionName)) {
            return null;
        }

        // The FunctionReference must be inside an ArrayCreationExpression (the args array)
        PsiElement arrayValueWrapper = functionReference.getParent();
        PsiElement argsArrayCandidate = (arrayValueWrapper != null) ? arrayValueWrapper.getParent() : null;
        if (!(argsArrayCandidate instanceof ArrayCreationExpression argsArray)) {
            return null;
        }

        // The array must be the first argument of ->args(...)
        PsiElement argsParameterList = argsArray.getParent();
        if (!(argsParameterList instanceof ParameterList)) {
            return null;
        }

        PsiElement argsMethodRef = argsParameterList.getParent();
        if (!(argsMethodRef instanceof MethodReference argsMethod) || !"args".equals(argsMethod.getName())) {
            return null;
        }

        // Verify the chain resolves to services(), not parameters()
        if ("parameters".equals(getFluentConfiguratorType(argsMethod.getClassReference(), new HashSet<>()))) {
            return null;
        }

        // Walk the chain to find the ->set(...) call
        MethodReference setMethod = getFluentSetMethodForArgs(argsMethod);
        if (setMethod == null) {
            return null;
        }

        String serviceClass = getFluentServiceClassFromSet(setMethod);
        if (StringUtils.isBlank(serviceClass)) {
            return null;
        }

        PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(element.getProject(), serviceClass, lazyServiceCollector);
        if (phpClass == null) {
            return null;
        }

        Method constructor = phpClass.getConstructor();
        if (constructor == null) {
            return null;
        }

        int parameterIndex = getFluentArgsArrayIndex(argsArray, functionReference);
        return new ServiceTypeHint(constructor, parameterIndex, element);
    }

    @Nullable
    private static MethodReference getFluentSetMethodForArgs(@NotNull MethodReference argsMethod) {
        PsiElement current = argsMethod.getClassReference();
        while (current instanceof MethodReference methodRef) {
            if ("set".equals(methodRef.getName())) {
                return methodRef;
            }
            current = methodRef.getClassReference();
        }
        return null;
    }

    private static int getFluentArgsArrayIndex(
        @NotNull ArrayCreationExpression argsArray,
        @NotNull PsiElement valueElement
    ) {
        PsiElement[] values = PhpElementsUtil.getArrayValues(argsArray);
        for (int i = 0; i < values.length; i++) {
            PsiElement val = values[i];
            if (PsiTreeUtil.isAncestor(val, valueElement, false) || val == valueElement) {
                return i;
            }
        }
        return 0;
    }

    @Nullable
    private static String getFluentServiceClassFromSet(@NotNull MethodReference setMethod) {
        PsiElement[] params = setMethod.getParameters();
        if (params.length >= 2) {
            return PhpArrayServiceUtil.getPsiValue(params[1]);
        }
        return null;
    }

    /**
     * foo:
     *  class: Foo
     *  arguments: [@<caret>]
     *  arguments:
     *      - @<caret>
     */
    @Nullable
    public static ServiceTypeHint getYamlConstructorTypeHint(@NotNull YAMLScalar yamlScalar, @NotNull ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {
        PsiElement context = yamlScalar.getContext();
        if(context instanceof YAMLKeyValue) {
            // arguments: ['$foobar': '@foo']

            String parameterName = ((YAMLKeyValue) context).getKeyText();
            if(parameterName.startsWith("$") && parameterName.length() > 1) {
                PsiElement yamlMapping = context.getParent();
                if(yamlMapping instanceof YAMLMapping) {
                    PsiElement yamlKeyValue = yamlMapping.getParent();
                    if(yamlKeyValue instanceof YAMLKeyValue) {
                        String keyText = ((YAMLKeyValue) yamlKeyValue).getKeyText();
                        if(keyText.equals("arguments")) {
                            YAMLMapping parentMapping = ((YAMLKeyValue) yamlKeyValue).getParentMapping();
                            if(parentMapping != null) {
                                String serviceId = getServiceClassFromServiceMapping(parentMapping);
                                if(StringUtils.isNotBlank(serviceId)) {
                                    PhpClass serviceClass = ServiceUtil.getResolvedClassDefinition(yamlScalar.getProject(), serviceId, lazyServiceCollector);
                                    if(serviceClass != null) {
                                        Method constructor = serviceClass.getConstructor();
                                        if(constructor != null) {
                                            int parameterIndex = PhpElementsUtil.getFunctionArgumentByName(constructor, StringUtils.stripStart(parameterName, "$"));
                                            if(parameterIndex >= 0) {
                                                return new ServiceTypeHint(constructor, parameterIndex, yamlScalar);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if(context instanceof YAMLSequenceItem sequenceItem) {
            // arguments: ['@foobar']

            PsiElement yamlSequenceItem = sequenceItem.getContext();
            if(yamlSequenceItem instanceof YAMLSequence) {
                YAMLSequence yamlArray = (YAMLSequence) sequenceItem.getContext();
                PsiElement yamlKeyValue = yamlArray.getContext();
                if(yamlKeyValue instanceof YAMLKeyValue yamlKeyValueArguments) {
                    if(yamlKeyValueArguments.getKeyText().equals("arguments")) {
                        YAMLMapping parentMapping = yamlKeyValueArguments.getParentMapping();
                        if(parentMapping != null) {
                            String serviceId = getServiceClassFromServiceMapping(parentMapping);
                            if(StringUtils.isNotBlank(serviceId)) {
                                PhpClass serviceClass = ServiceUtil.getResolvedClassDefinition(yamlScalar.getProject(), serviceId, lazyServiceCollector);
                                if(serviceClass != null) {
                                    Method constructor = serviceClass.getConstructor();
                                    if(constructor != null) {
                                        return new ServiceTypeHint(
                                            constructor,
                                            PsiElementUtils.getPrevSiblingsOfType(sequenceItem, PlatformPatterns.psiElement(YAMLSequenceItem.class)).size(),
                                            yamlScalar
                                        );
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * arguments: ['$foobar': '@foo']
     */
    @Nullable
    public static Parameter getYamlNamedArgument(@NotNull PsiElement psiElement, @NotNull ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {
        PsiElement context = psiElement.getContext();
        if(context instanceof YAMLKeyValue) {
            // arguments: ['$foobar': '@foo']

            String parameterName = ((YAMLKeyValue) context).getKeyText();
            if(parameterName.startsWith("$") && parameterName.length() > 1) {
                PsiElement yamlMapping = context.getParent();
                if(yamlMapping instanceof YAMLMapping) {
                    PsiElement yamlKeyValue = yamlMapping.getParent();
                    if(yamlKeyValue instanceof YAMLKeyValue) {
                        String keyText = ((YAMLKeyValue) yamlKeyValue).getKeyText();
                        if(keyText.equals("arguments")) {
                            YAMLMapping parentMapping = ((YAMLKeyValue) yamlKeyValue).getParentMapping();
                            if(parentMapping != null) {
                                String serviceId = getServiceClassFromServiceMapping(parentMapping);
                                if(StringUtils.isNotBlank(serviceId)) {
                                    PhpClass serviceClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), serviceId, lazyServiceCollector);
                                    if(serviceClass != null) {
                                        return PhpElementsUtil.getConstructorParameterArgumentByName(serviceClass, StringUtils.stripStart(parameterName, "$"));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * arguments: ['$foobar': '@foo']
     */
    public static boolean hasMissingYamlNamedArgumentForInspection(@NotNull PsiElement psiElement, @NotNull ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {
        PsiElement context = psiElement.getContext();
        if(context instanceof YAMLKeyValue) {
            // arguments: ['$foobar': '@foo']

            String parameterName = ((YAMLKeyValue) context).getKeyText();
            if(parameterName.startsWith("$") && parameterName.length() > 1) {
                PsiElement yamlMapping = context.getParent();
                if(yamlMapping instanceof YAMLMapping) {
                    PsiElement yamlKeyValue = yamlMapping.getParent();
                    if(yamlKeyValue instanceof YAMLKeyValue) {
                        String keyText = ((YAMLKeyValue) yamlKeyValue).getKeyText();
                        if(keyText.equals("arguments")) {
                            YAMLMapping parentMapping = ((YAMLKeyValue) yamlKeyValue).getParentMapping();
                            if(parentMapping != null) {
                                String serviceId = getServiceClassFromServiceMapping(parentMapping);
                                if(StringUtils.isNotBlank(serviceId)) {
                                    PhpClass serviceClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), serviceId, lazyServiceCollector);
                                    // class not found don't need a hint
                                    if (serviceClass == null) {
                                        return false;
                                    }

                                    return PhpElementsUtil.getConstructorParameterArgumentByName(serviceClass, StringUtils.stripStart(parameterName, "$")) == null;
                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * services:
     *  _defaults:
     *      bind:
     *       $<caret>: ''
     *
     * services:
     *    Foo:
     *       arguments:
     *         $<caret>
     */
    public static void visitNamedArguments(@NotNull PsiFile psiFile, @NotNull Consumer<Pair<Parameter, Integer>> processor) {
        if (psiFile instanceof YAMLFile) {
            Collection<Pair<Parameter, Integer>> parameters = new HashSet<>();

            // direct service definition
            for (PhpClass phpClass : YamlHelper.getPhpClassesInYamlFile((YAMLFile) psiFile, new ContainerCollectionResolver.LazyServiceCollector(psiFile.getProject()))) {
                Method constructor = phpClass.getConstructor();
                if (constructor == null) {
                    continue;
                }

                Parameter @NotNull [] methodParameters = constructor.getParameters();
                for (int i = 0, methodParametersLength = methodParameters.length; i < methodParametersLength; i++) {
                    parameters.add(Pair.create(methodParameters[i], i));
                }
            }

            for (YAMLKeyValue taggedService : YamlHelper.getTaggedServices((YAMLFile) psiFile, "controller.service_arguments")) {
                PsiElement key = taggedService.getKey();
                if (key == null) {
                    continue;
                }

                String keyText = key.getText();
                if (StringUtils.isBlank(keyText)) {
                    continue;
                }

                // App\Controller\ => \App\Controller
                String namespace = StringUtils.strip(keyText, "\\");
                for (PhpClass phpClass : PhpIndexUtil.getPhpClassInsideNamespace(psiFile.getProject(), "\\" + namespace)) {
                    // find all parameters on public methods; this are possible actions

                    // maybe filter actions and public methods in a suitable way?
                    phpClass.getMethods().stream()
                        .filter(method -> method.getAccess().isPublic() && !method.getName().startsWith("set"))
                        .forEach(method -> {
                            Parameter @NotNull [] methodParameters = method.getParameters();
                            for (int i = 0, methodParametersLength = methodParameters.length; i < methodParametersLength; i++) {
                                parameters.add(Pair.create(methodParameters[i], i));
                            }
                        });
                }
            }

            parameters.forEach(processor::consume);
        }
    }

    /*
     * Symfony 3.3: "class" is optional; use service name for its it
     *
     * Foo\Bar:
     *  arguments: ~
     */
    @Nullable
    public static String getServiceClassFromServiceMapping(@NotNull YAMLMapping yamlMapping) {
        YAMLKeyValue classKeyValue = yamlMapping.getKeyValueByKey("class");

        // Symfony 3.3: "class" is optional; use service id for class
        // Foo\Bar:
        //   arguments: ~
        if(classKeyValue != null) {
            return classKeyValue.getValueText();
        }

        PsiElement parent = yamlMapping.getParent();
        if(parent instanceof YAMLKeyValue) {
            String keyText = ((YAMLKeyValue) parent).getKeyText();
            if(YamlHelper.isClassServiceId(keyText)) {
                return keyText;
            }
        }

        return null;
    }

    @Nullable
    public static PhpClass getServicePhpClassFromServiceMapping(@NotNull YAMLMapping yamlMapping, @NotNull ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {
        String serviceId = getServiceClassFromServiceMapping(yamlMapping);
        if (StringUtils.isNotBlank(serviceId)) {
            return ServiceUtil.getResolvedClassDefinition(yamlMapping.getProject(), serviceId, lazyServiceCollector);
        }

        return null;
    }


    /**
     *  <services>
     *   <service class="Foo\\Bar\\Car">
     *    <argument type="service" id="<caret>" />
     *  </service>
     * </services>
     */
    @Nullable
    public static ServiceTypeHint getXmlConstructorTypeHint(@NotNull PsiElement psiElement, @NotNull ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {
        if(!(psiElement.getContainingFile() instanceof XmlFile) || psiElement.getNode().getElementType() != XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN) {
            return null;
        }

        XmlAttributeValue xmlAttributeValue = PsiTreeUtil.getParentOfType(psiElement, XmlAttributeValue.class);
        if(xmlAttributeValue == null) {
            return null;
        }

        XmlTag argumentTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
        if(argumentTag == null) {
            return null;
        }

        XmlTag serviceTag = PsiElementAssertUtil.getParentOfTypeOrNull(argumentTag, XmlTag.class);
        if(serviceTag == null || !"service".equals(serviceTag.getName())) {
            return null;
        }

        // service/argument[id]
        String serviceDefName = XmlHelper.getClassFromServiceDefinition(serviceTag);
        if(serviceDefName != null) {
            PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), serviceDefName, lazyServiceCollector);

            // check type hint on constructor
            if(phpClass != null) {
                Method constructor = phpClass.getConstructor();
                if(constructor != null) {
                    return new ServiceTypeHint(constructor, XmlHelper.getArgumentIndex(argumentTag, constructor), psiElement);
                }
            }
        }

        return null;
    }

    /**
     *  <services>
     *   <service class="Foo\\Bar\\Car">
     *    <call method="foo"></call>
     *      <argument type="service" id="<caret>" />
     *    </call>
     *  </service>
     * </services>
     */
    @Nullable
    public static ServiceTypeHint getXmlCallTypeHint(@NotNull PsiElement psiElement, @NotNull ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {
        // search for parent service definition
        XmlTag currentXmlTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
        XmlTag parentXmlTag = PsiTreeUtil.getParentOfType(currentXmlTag, XmlTag.class);
        if(parentXmlTag == null) {
            return null;
        }

        String name = parentXmlTag.getName();
        if(!"call".equals(name)) {
            return null;
        }

        // service/call/argument[id]
        XmlAttribute methodAttribute = parentXmlTag.getAttribute("method");
        if(methodAttribute != null) {
            String methodName = methodAttribute.getValue();
            XmlTag serviceTag = parentXmlTag.getParentTag();

            // get service class
            if(serviceTag != null && "service".equals(serviceTag.getName())) {
                String serviceDefName = XmlHelper.getClassFromServiceDefinition(serviceTag);
                if(serviceDefName != null) {
                    PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), serviceDefName, lazyServiceCollector);

                    // finally check method type hint
                    if(phpClass != null) {
                        Method method = phpClass.findMethodByName(methodName);
                        if(method != null) {
                            return new ServiceTypeHint(method, XmlHelper.getArgumentIndex(currentXmlTag, method), psiElement);
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Foobar::CONST
     */
    @NotNull
    public static Collection<PsiElement> getTargetsForConstant(@NotNull Project project, @NotNull String contents) {
        // FOO
        if (!contents.contains(":")) {
            if(!contents.startsWith("\\")) {
                contents = "\\" + contents;
            }

            return new ArrayList<>(
                PhpIndex.getInstance(project).getConstantsByFQN(contents)
            );
        }

        contents = contents.replaceAll(":+", ":");
        String[] split = contents.split(":");

        if (split.length < 2) {
            // Empty const name e.g. "\\App\\Foo::"
            return Collections.emptyList();
        }

        Collection<PsiElement> psiElements = new ArrayList<>();
        for (PhpClass phpClass : PhpElementsUtil.getClassesInterface(project, split[0])) {
            Field fieldByName = phpClass.findFieldByName(split[1], true);
            if(fieldByName != null && fieldByName.isConstant()) {
                psiElements.add(fieldByName);
            }
        }

        return psiElements;
    }

    /**
     * Calculate usage as of given service id in project scope
     */
    public static int getServiceUsage(@NotNull Project project, @NotNull String id) {
        int usage = 0;

        List<Integer> values = FileBasedIndex.getInstance().getValues(ContainerIdUsagesStubIndex.KEY, id, GlobalSearchScope.allScope(project));
        for (Integer integer : values) {
            usage += integer;
        }

        return usage;
    }

    /**
     * Move services done which are possible "garbage" or should not be taken like ".debug"
     *
     * - ".1_~NpzP6Xn"
     *  - ".debug."
     *  - "router.debug"
     */
    public static boolean isLowerPriority(@NotNull String name) {
        for (String lowerName: LOWER_PRIORITY) {
            // reduce the
            // - ".1_~NpzP6Xn"
            // -
            if (name.startsWith(".") || name.contains("~") || name.toLowerCase().contains(lowerName)) {
                return true;
            }
        }

        return false;
    }

    public static class ContainerServiceIdPriorityNameComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {

            if(isLowerPriority(o1) && isLowerPriority(o2)) {
                return 0;
            }

            return isLowerPriority(o1) ? 1 : -1;
        }
    }

    @NotNull
    public static List<String> getSortedServiceId(@NotNull Project project, @NotNull Collection<String> ids) {
        if(ids.isEmpty()) {
            return new ArrayList<>(ids);
        }

        List<String> myIds = new ArrayList<>(ids);

        myIds.sort(new ServiceContainerUtil.ContainerServiceIdPriorityNameComparator());

        myIds.sort((o1, o2) ->
            Integer.compare(ServiceContainerUtil.getServiceUsage(project, o2), ServiceContainerUtil.getServiceUsage(project, o1))
        );

        return myIds;
    }

    /**
     * Find compiled service container XML files.
     *
     * - "app/cache/dev/appDevDebugProjectContainer.xml"
     * - ...
     *
     * Cache invalidation is handled by SymfonyVarDirectoryWatcher via VFS events.
     */
    public static Collection<String> getContainerFiles(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            SYMFONY_CONTAINER_FILES,
            () -> CachedValueProvider.Result.create(
                Collections.unmodifiableSet(getContainerFilesInner(project)),
                getContainerTracker(project)
            ),
            false
        );
    }

    private static SimpleModificationTracker getContainerTracker(@NotNull Project project) {
        return SymfonyVarDirectoryWatcherKt.getSymfonyVarDirectoryWatcher(project)
            .getModificationTracker(SymfonyVarDirectoryWatcher.Scope.CONTAINER);
    }

    /**
     * All class that matched against this pattern
     *
     * My<caret>Class\:
     *  resource: '....'
     *  exclude: '....'
     */
    @NotNull
    public static Collection<String> getPhpClassFromResources(@NotNull Project project, @NotNull String namespace, @NotNull VirtualFile containerFile, @NotNull Collection<String> resources, @NotNull Collection<String> excludes) {
        Collection<String> phpClasses = new HashSet<>();

        // Normalize namespace for string matching (ensure backslash prefix)
        String normalizedNamespace = namespace.startsWith("\\") ? namespace : "\\" + namespace;

        for (String resource : resources) {
            if (StringUtils.isBlank(resource)) {
                continue;
            }

            // Step 1: Resolve base directory from resource pattern
            VirtualFile baseDirectory = resolveBaseDirectoryFromResourcePattern(resource, containerFile);

            // Step 2: Collect matching FQNs via index (lightweight string matching, no PSI loading)

            if (baseDirectory == null) {
                continue;
            }

            ServiceResourceGlobMatcher globMatcher = ServiceResourceGlobMatcher.create(
                containerFile,
                Collections.singletonList(resource),
                excludes
            );

            // resource: src/* vs src/test.php
            GlobalSearchScope scope = baseDirectory.isDirectory()
                ? GlobalSearchScopesCore.directoriesScope(project, true, baseDirectory)
                : GlobalSearchScope.fileScope(project, baseDirectory);

            for (PhpClassResourceCandidate candidate : getPhpClassFqnsForResourceScope(project, normalizedNamespace, baseDirectory, scope)) {
                if (!globMatcher.matches(candidate.virtualFile())) {
                    continue;
                }

                phpClasses.add(candidate.fqn());
            }
        }

        return phpClasses;
    }

    /**
     * Cached resource-scope candidates with concrete class FQN and file path for glob filtering.
     */
    @NotNull
    private static Collection<PhpClassResourceCandidate> getPhpClassFqnsForResourceScope(
        @NotNull Project project,
        @NotNull String normalizedNamespace,
        @NotNull VirtualFile baseDirectory,
        @NotNull GlobalSearchScope scope
    ) {
        Map<String, Collection<PhpClassResourceCandidate>> cache = CachedValuesManager.getManager(project).getCachedValue(
            project,
            PHP_CLASS_FQN_RESOURCE_CACHE,
            () -> CachedValueProvider.Result.create(
                new ConcurrentHashMap<>(),
                FileIndexCaches.getModificationTrackerForIndexId(project, PhpClassFqnIndex.KEY)
            ),
            false
        );

        String cacheKey = normalizedNamespace + "|" + baseDirectory.getPath();

        return cache.computeIfAbsent(cacheKey, key -> {
            PhpIndex phpIndex = PhpIndex.getInstance(project);
            Set<PhpClassResourceCandidate> candidates = new HashSet<>();
            FileBasedIndex.getInstance().processAllKeys(PhpClassFqnIndex.KEY, fqn -> {
                if (fqn.startsWith(normalizedNamespace)) {
                    boolean hasConcreteClass = phpIndex.getClassesByFQN(fqn).stream().anyMatch(phpClass -> !phpClass.isAbstract());
                    if (!hasConcreteClass) {
                        return true;
                    }

                    for (VirtualFile virtualFile : FileBasedIndex.getInstance().getContainingFiles(PhpClassFqnIndex.KEY, fqn, scope)) {
                        candidates.add(new PhpClassResourceCandidate(fqn, virtualFile));
                    }
                }
                return true;
            }, scope, null);

            return candidates;
        });
    }

    private record PhpClassResourceCandidate(@NotNull String fqn, @NotNull VirtualFile virtualFile) {
    }

    /**
     * Extract the base directory from a resource glob pattern.
     *
     * Examples:
     *   "../src/" → "../src/"
     *   "../src/*" → "../src/"
     *   "../src/{Foo,Bar}/*" → "../src/"
     *   "../../{DependencyInjection,Tests}" → "../../"
     */
    @Nullable
    public static VirtualFile resolveBaseDirectoryFromResourcePattern(
        @NotNull String resourcePattern,
        @NotNull VirtualFile containerFile
    ) {
        String normalizedPath = resourcePattern.replace("\\", "/");

        // Find the first glob special character
        int globIndex = -1;
        for (int i = 0; i < normalizedPath.length(); i++) {
            char c = normalizedPath.charAt(i);
            if (c == '*' || c == '{' || c == '?') {
                globIndex = i;
                break;
            }
        }

        String basePath;
        if (globIndex > 0) {
            // Extract path up to the last directory separator before glob
            String beforeGlob = normalizedPath.substring(0, globIndex);
            int lastSlash = beforeGlob.lastIndexOf('/');
            basePath = lastSlash > 0 ? beforeGlob.substring(0, lastSlash) : beforeGlob;
        } else {
            basePath = normalizedPath;
        }

        // Remove trailing slash
        basePath = StringUtils.stripEnd(basePath, "/");

        // Handle ".." relative paths
        VirtualFile baseDir = containerFile.getParent();
        String[] parts = basePath.split("/");
        for (String part : parts) {
            if ("..".equals(part)) {
                baseDir = baseDir != null ? baseDir.getParent() : null;
            } else if (StringUtils.isNotBlank(part) && !".".equals(part)) {
                baseDir = baseDir != null ? baseDir.findChild(part) : null;
            }
            if (baseDir == null) {
                return null;
            }
        }

        return baseDir;
    }

    /**
     * Find possible compiled service file
     *
     * - "app/cache/dev/appDevDebugProjectContainer.xml"
     * - "var/cache/dev/appDevDebugProjectContainer.xml"
     * - "var/cache/dev/srcDevDebugProjectContainer.xml"
     * - "var/cache/dev/srcApp_KernelDevDebugContainer.xml"
     * - "var/cache/dev/App_KernelDevDebugContainer.xml" // Symfony => 4 + flex
     * - "app/cache/dev_392373729/appDevDebugProjectContainer.xml"
     */
    private static Set<String> getContainerFilesInner(@NotNull Project project) {
        Set<String> files = new HashSet<>();

        VirtualFile baseDir = ProjectUtil.getProjectDir(project);
        if (baseDir == null) {
            return files;
        }

        // several Symfony cache folder structures
        for (String root : new String[] {"var/cache", "app/cache"}) {
            VirtualFile relativeFile = VfsUtil.findRelativeFile(root, baseDir);
            if (relativeFile == null) {
                continue;
            }

            // find a dev folder eg: "dev_392373729" or just "dev"
            for (VirtualFile child : relativeFile.getChildren()) {
                if (!child.isDirectory() || !child.getName().toLowerCase().startsWith("dev")) {
                    continue;
                }

                for (VirtualFile file : child.getChildren()) {
                    if (file.isDirectory() || !"xml".equalsIgnoreCase(file.getExtension())) {
                        continue;
                    }

                    // Some examples: App_KernelDevDebugContainer, appDevDebugProjectContainer
                    String filename = file.getName().toLowerCase();
                    if (filename.contains("debugcontainer")
                        || (filename.contains("debug") && filename.contains("container"))
                        || (filename.contains("kernel") && filename.contains("container"))) {
                        String path = VfsUtil.getRelativePath(file, baseDir, '/');
                        if (path != null) {
                            files.add(path.replace('\\', '/'));
                        }
                    }
                }
            }
        }

        return files;
    }
}
