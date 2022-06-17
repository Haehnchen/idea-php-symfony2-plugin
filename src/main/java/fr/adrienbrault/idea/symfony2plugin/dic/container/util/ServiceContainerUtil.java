package fr.adrienbrault.idea.symfony2plugin.dic.container.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.*;
import com.intellij.psi.xml.*;
import com.intellij.util.Consumer;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.refactoring.PhpNamespaceBraceConverter;
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
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ContainerIdUsagesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.*;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PsiElementAssertUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceContainerUtil {
    public static MethodMatcher.CallToSignature[] SERVICE_GET_SIGNATURES = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\DependencyInjection\\ContainerInterface", "get"),
        new MethodMatcher.CallToSignature("\\Psr\\Container\\ContainerInterface", "get"),
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "get"),

        // Symfony 3.3 / 3.4
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\ControllerTrait", "get"),

        // Symfony 4
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController", "get"),

        new MethodMatcher.CallToSignature("\\Symfony\\Component\\DependencyInjection\\ParameterBag\\ContainerBagInterface", "get"),
    };

    private static final Key<CachedValue<Collection<String>>> SYMFONY_COMPILED_TIMED_SERVICE_WATCHER = new Key<>("SYMFONY_COMPILED_TIMED_SERVICE_WATCHER");
    private static final Key<CachedValue<Collection<String>>> SYMFONY_COMPILED_SERVICE_WATCHER = new Key<>("SYMFONY_COMPILED_SERVICE_WATCHER");

    private static String[] LOWER_PRIORITY = new String[] {
        "debug", "default", "abstract", "inner", "chain", "decorate", "delegat"
    };

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
            visitFile((PhpFile) psiFile, serviceConsumer -> {
                SerializableService serializableService = createService(serviceConsumer);
                services.add(serializableService);
            });
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

        XmlTag xmlTags[] = PsiTreeUtil.getChildrenOfType(psiFile.getFirstChild(), XmlTag.class);
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
    private static String getStringValueIndexSafe(@NotNull PsiElement psiElement) {
        String serviceClass = null;
        if (psiElement instanceof StringLiteralExpression) {
            serviceClass = ((StringLiteralExpression) psiElement).getContents();
        } else if(psiElement instanceof ClassConstantReference) {
            serviceClass = PhpElementsUtil.getClassConstantPhpFqn((ClassConstantReference) psiElement);
        }

        return StringUtils.isNotBlank(serviceClass)
            ? serviceClass
            : null;
    }

    /**
     * return static function (ContainerConfigurator $container) { ... }
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

            for (PhpReturn phpReturn : PsiTreeUtil.collectElementsOfType(phpNamespace, PhpReturn.class)) {
                for (Function function : PsiTreeUtil.collectElementsOfType(phpReturn, Function.class)) {
                    Parameter parameter = function.getParameter(0);
                    if (parameter == null) {
                        continue;
                    }

                    // \Symfony\Component\DependencyInjection\Loader\Configurator\ContainerConfigurator
                    if(parameter.getLocalType().getTypes().stream().noneMatch(s -> s.contains("\\ContainerConfigurator"))) {
                        continue;
                    }

                    functions.add(function);
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
        for (Function function : getPhpContainerConfiguratorFunctions(phpFile)) {
            // we only want "set" and "alias" methods
            PsiElement[] methodReferences = PsiTreeUtil.collectElements(
                function,
                psiElement -> psiElement instanceof MethodReference && ("set".equals(((MethodReference) psiElement).getName()) || "alias".equals(((MethodReference) psiElement).getName()))
            );

            for (PsiElement psiElement : methodReferences) {
                // ->set('translator.default', Translator::class)
                if (psiElement instanceof MethodReference && "set".equals(((MethodReference) psiElement).getName())) {
                    PsiElement[] parameters = ((MethodReference) psiElement).getParameters();
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

                    consumer.consume(new ServiceConsumer(parameters[0], serviceName, new PhpKeyValueAttributeValue(psiElement, keyValue), ServiceFileDefaults.EMPTY));
                }

                // ->alias(TranslatorInterface::class, 'translator')
                if (psiElement instanceof MethodReference && "alias".equals(((MethodReference) psiElement).getName())) {
                    PsiElement[] parameters = ((MethodReference) psiElement).getParameters();
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

                    consumer.consume(new ServiceConsumer(parameters[0], serviceName, new PhpKeyValueAttributeValue(psiElement, keyValue), ServiceFileDefaults.EMPTY));
                }
            }
        }
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

        switch (value.toLowerCase()) {
            case "false":
                return false;
            case "true":
                return true;
            default:
                return null;
        }

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
        } else if(context instanceof YAMLSequenceItem) {
            // arguments: ['@foobar']

            YAMLSequenceItem sequenceItem = (YAMLSequenceItem) context;
            PsiElement yamlSequenceItem = sequenceItem.getContext();
            if(yamlSequenceItem instanceof YAMLSequence) {
                YAMLSequence yamlArray = (YAMLSequence) sequenceItem.getContext();
                PsiElement yamlKeyValue = yamlArray.getContext();
                if(yamlKeyValue instanceof YAMLKeyValue) {
                    YAMLKeyValue yamlKeyValueArguments = (YAMLKeyValue) yamlKeyValue;
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
     */
    public static void visitNamedArguments(@NotNull PsiFile psiFile, @NotNull Consumer<Parameter> processor) {
        if (psiFile instanceof YAMLFile) {
            Collection<Parameter> parameters = new HashSet<>();

            // direct service definition
            for (PhpClass phpClass : YamlHelper.getPhpClassesInYamlFile((YAMLFile) psiFile, new ContainerCollectionResolver.LazyServiceCollector(psiFile.getProject()))) {
                Method constructor = phpClass.getConstructor();
                if (constructor == null) {
                    continue;
                }

                parameters.addAll(Arrays.asList(constructor.getParameters()));
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
                        .forEach(method -> Collections.addAll(parameters, method.getParameters()));
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
        if(ids.size() == 0) {
            return new ArrayList<>(ids);
        }

        List<String> myIds = new ArrayList<>(ids);

        myIds.sort(new ServiceContainerUtil.ContainerServiceIdPriorityNameComparator());

        myIds.sort((o1, o2) ->
            ((Integer) ServiceContainerUtil.getServiceUsage(project, o2))
                .compareTo(ServiceContainerUtil.getServiceUsage(project, o1))
        );

        return myIds;
    }

    /**
     * Find compiled and cache it until any psi change occur
     *
     * - "app/cache/dev/appDevDebugProjectContainer.xml"
     * - ...
     */
    public static Collection<String> getContainerFiles(@NotNull Project project) {
        return CachedValuesManager.getManager(project)
            .getCachedValue(
                project,
                SYMFONY_COMPILED_SERVICE_WATCHER,
                () -> CachedValueProvider.Result.create(getContainerFilesInner(project), PsiModificationTracker.MODIFICATION_COUNT),
                false
            );
    }

    /**
     * All class that matched against this pattern
     *
     * My<caret>Class\:
     *  resource: '....'
     *  exclude: '....'
     */
    @NotNull
    public static Collection<PhpClass> getPhpClassFromResources(@NotNull Project project, @NotNull String namespace, @NotNull VirtualFile containerFile, @NotNull Collection<String> resource, @NotNull Collection<String> exclude) {
        Collection<PhpClass> phpClasses = new HashSet<>();

        for (PhpClass phpClass : PhpIndexUtil.getPhpClassInsideNamespace(project, "\\" + StringUtils.strip(namespace, "\\"))) {
            boolean classMatchesGlob = ServiceIndexUtil.matchesResourcesGlob(
                containerFile,
                phpClass.getContainingFile().getVirtualFile(),
                resource,
                exclude
            );

            if (classMatchesGlob) {
                phpClasses.add(phpClass);
            }
        }

        return phpClasses;
    }

    /**
     * Find possible compiled service file with seconds cache
     *
     * - "app/cache/dev/appDevDebugProjectContainer.xml"
     * - "var/cache/dev/appDevDebugProjectContainer.xml"
     * - "var/cache/dev/srcDevDebugProjectContainer.xml"
     * - "var/cache/dev/srcApp_KernelDevDebugContainer.xml"
     * - "var/cache/dev/App_KernelDevDebugContainer.xml" // Symfony => 4 + flex
     * - "app/cache/dev_392373729/appDevDebugProjectContainer.xml"
     */
    private static Collection<String> getContainerFilesInner(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(project, SYMFONY_COMPILED_TIMED_SERVICE_WATCHER, () -> {
            Set<String> files = new HashSet<>();

            VirtualFile baseDir = ProjectUtil.getProjectDir(project);

            // several Symfony cache folder structures
            for (String root : new String[] {"var/cache", "app/cache"}) {
                VirtualFile relativeFile = VfsUtil.findRelativeFile(root, baseDir);
                if (relativeFile == null) {
                    continue;
                }

                // find a dev folder eg: "dev_392373729" or just "dev"
                Set<VirtualFile> devFolders = Stream.of(relativeFile.getChildren())
                    .filter(virtualFile -> virtualFile.isDirectory() && virtualFile.getName().toLowerCase().startsWith("dev"))
                    .collect(Collectors.toSet());

                for (VirtualFile devFolder : devFolders) {
                    Set<String> debugContainers = Stream.of(devFolder.getChildren())
                        .filter(virtualFile -> {
                            if (!"xml".equalsIgnoreCase(virtualFile.getExtension())) {
                                return false;
                            }

                            // Some examples: App_KernelDevDebugContainer, appDevDebugProjectContainer
                            String filename = virtualFile.getName().toLowerCase();
                            return filename.contains("debugcontainer")
                                || (filename.contains("debug") && filename.contains("container"))
                                || (filename.contains("kernel") && filename.contains("container"));
                        })
                        .map(virtualFile -> VfsUtil.getRelativePath(virtualFile, baseDir, '/'))
                        .collect(Collectors.toSet());

                    files.addAll(debugContainers);
                }
            }

            Set<String> cache = files.stream().map(s -> baseDir.getPath() + "/" + s).collect(Collectors.toSet());

            return CachedValueProvider.Result.create(Collections.unmodifiableSet(files), new AbsoluteFileModificationTracker(cache));
        }, false);
    }
}
