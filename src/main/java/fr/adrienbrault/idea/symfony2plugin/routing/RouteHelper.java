package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.xml.XmlDocumentImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.*;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.refactoring.PhpNameUtil;
import de.espend.idea.php.annotation.dict.PhpDocCommentAnnotation;
import de.espend.idea.php.annotation.dict.PhpDocTagAnnotation;
import de.espend.idea.php.annotation.util.AnnotationUtil;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.extension.RoutingLoader;
import fr.adrienbrault.idea.symfony2plugin.extension.RoutingLoaderParameter;
import fr.adrienbrault.idea.symfony2plugin.routing.dic.ControllerClassOnShortcutReturn;
import fr.adrienbrault.idea.symfony2plugin.routing.dic.ServiceRouteContainer;
import fr.adrienbrault.idea.symfony2plugin.routing.dict.RoutesContainer;
import fr.adrienbrault.idea.symfony2plugin.routing.dict.RoutingFile;
import fr.adrienbrault.idea.symfony2plugin.stubs.SymfonyProcessors;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.StubIndexedRoute;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.RoutesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.*;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerAction;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.*;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteHelper {

    private static final Key<CachedValue<Map<String, Route>>> ROUTE_CACHE = new Key<>("SYMFONY:ROUTE_CACHE");

    public static Set<String> ROUTE_CLASSES = new HashSet<>(Arrays.asList(
        "Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Route",
        "Symfony\\Component\\Routing\\Annotation\\Route"
    ));

    public static Map<Project, Map<String, RoutesContainer>> COMPILED_CACHE = new HashMap<>();

    private static final ExtensionPointName<RoutingLoader> ROUTING_LOADER = new ExtensionPointName<>(
        "fr.adrienbrault.idea.symfony2plugin.extension.RoutingLoader"
    );

    public static LookupElement[] getRouteParameterLookupElements(@NotNull Project project, @NotNull String routeName) {
        List<LookupElement> lookupElements = new ArrayList<>();

        for (Route route : RouteHelper.getRoute(project, routeName)) {
            for(String values: route.getVariables()) {
                lookupElements.add(LookupElementBuilder.create(values).withIcon(Symfony2Icons.ROUTE));
            }
        }

        if (SymfonyUtil.isVersionGreaterThen(project, "3.2.0")) {
            lookupElements.add(LookupElementBuilder.create("_fragment").withIcon(Symfony2Icons.ROUTE));
        }

        return lookupElements.toArray(new LookupElement[lookupElements.size()]);
    }

    @NotNull
    public static Collection<Route> getRoute(@NotNull Project project, @NotNull String routeName) {
        Map<String, Route> compiledRoutes = RouteHelper.getCompiledRoutes(project);
        if(compiledRoutes.containsKey(routeName)) {
            return Collections.singletonList(compiledRoutes.get(routeName));
        }

        Collection<Route> routes = new ArrayList<>();

        Collection<VirtualFile> routeFiles = FileBasedIndex.getInstance().getContainingFiles(RoutesStubIndex.KEY, routeName, GlobalSearchScope.allScope(project));
        for(StubIndexedRoute route: FileBasedIndex.getInstance().getValues(RoutesStubIndex.KEY, routeName, GlobalSearchScope.filesScope(project, routeFiles))) {
            routes.add(new Route(route));
        }

        return routes;
    }

    public static PsiElement[] getRouteParameterPsiElements(Project project, String routeName, String parameterName) {

        List<PsiElement> results = new ArrayList<>();

        for (PsiElement psiElement : RouteHelper.getMethods(project, routeName)) {

            if(psiElement instanceof Method) {
                for(Parameter parameter: ((Method) psiElement).getParameters()) {
                    if(parameter.getName().equals(parameterName)) {
                        results.add(parameter);
                    }
                }
            }

        }

        return results.toArray(new PsiElement[results.size()]);

    }

    @NotNull
    public static PsiElement[] getMethods(@NotNull Project project, @NotNull String routeName) {
        Collection<PsiElement> targets = new ArrayList<>();

        for (Route route : getRoute(project, routeName)) {
            targets.addAll(Arrays.asList(getMethodsOnControllerShortcut(project, route.getController())));
        }

        return targets.toArray(new PsiElement[targets.size()]);
    }

    /**
     * convert to controller name to method:
     *
     * FooBundle\Controller\BarController::fooBarAction
     * foo_service_bar:fooBar
     * AcmeDemoBundle:Demo:hello
     * FooBundle\Controller\BarController (__invoke)
     *
     * @param project current project
     * @param controllerName controller service, raw or compiled
     * @return targets
     */
    @NotNull
    public static PsiElement[] getMethodsOnControllerShortcut(@NotNull Project project, @Nullable String controllerName) {
        if(controllerName == null)  {
            return new PsiElement[0];
        }

        if(controllerName.contains("::")) {

            // FooBundle\Controller\BarController::fooBarAction
            String className = controllerName.substring(0, controllerName.lastIndexOf("::"));
            String methodName = controllerName.substring(controllerName.lastIndexOf("::") + 2);

            Method method = PhpElementsUtil.getClassMethod(project, className, methodName);
            return method != null ? new PsiElement[] {method} : new PsiElement[0];

        } else if(controllerName.contains(":")) {
            // AcmeDemoBundle:Demo:hello
            String[] split = controllerName.split(":");
            if(split.length == 3) {
                Collection<Method> controllerMethod = ControllerIndex.getControllerMethod(project, controllerName);
                if(controllerMethod.size() > 0) {
                    return controllerMethod.toArray(new PsiElement[controllerMethod.size()]);
                }
            }

            // foo_service_bar:fooBar
            ControllerAction controllerServiceAction = new ControllerIndex(project).getControllerActionOnService(controllerName);
            if(controllerServiceAction != null) {
                return new PsiElement[] {controllerServiceAction.getMethod()};
            }

        } else if(PhpNameUtil.isValidNamespaceFullName(controllerName, true)) {
            // FooBundle\Controller\BarController (__invoke)
            Method invoke = PhpElementsUtil.getClassMethod(project, controllerName, "__invoke");
            if(invoke != null) {
                return new PsiElement[] {invoke};
            }

            // class fallback
            Collection<PhpClass> phpClass = PhpElementsUtil.getClassesInterface(project, controllerName);
            return phpClass.toArray(new PsiElement[phpClass.size()]);
        }

        return new PsiElement[0];
    }

    /**
     * convert to controller class:
     *
     * FooBundle\Controller\BarController::fooBarAction
     * foo_service_bar:fooBar
     * AcmeDemoBundle:Demo:hello
     *
     * @param project current project
     * @param controllerName controller service, raw or compiled
     * @return targets
     */
    @Nullable
    public static ControllerClassOnShortcutReturn getControllerClassOnShortcut(@NotNull Project project,@NotNull  String controllerName) {

        if(controllerName.contains("::")) {
            // FooBundle\Controller\BarController::fooBarAction

            PhpClass aClass = PhpElementsUtil.getClass(project, controllerName.substring(0, controllerName.lastIndexOf("::")));
            if(aClass != null) {
                return new ControllerClassOnShortcutReturn(aClass);
            }

            return null;
        }

        // AcmeDemoBundle:Demo:hello
        String[] split = controllerName.split(":");
        if(split.length == 3) {
            // try to resolve on bundle path
            for (SymfonyBundle symfonyBundle : new SymfonyBundleUtil(project).getBundle(split[0])) {
                PhpClass aClass = PhpElementsUtil.getClass(project, symfonyBundle.getNamespaceName() + "Controller\\" + split[1] + "Controller");
                // @TODO: support multiple bundle names
                if(aClass != null) {
                    return new ControllerClassOnShortcutReturn(aClass);
                }
            }
        } else if(split.length == 2) {
            // controller as service:
            // foo_service_bar:fooBar
            PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(project, split[0]);
            if(phpClass != null) {
                return new ControllerClassOnShortcutReturn(phpClass, true);
            }
        }

        return null;
    }

    private static <E> ArrayList<E> makeCollection(Iterable<E> iter) {
        ArrayList<E> list = new ArrayList<>();
        for (E item : iter) {
            list.add(item);
        }
        return list;
    }

    private static String getPath(Project project, String path) {
        if (!FileUtil.isAbsolute(path)) { // Project relative path
            path = project.getBasePath() + "/" + path;
        }

        return path;
    }

    @NotNull
    public static Map<String, Route> getCompiledRoutes(@NotNull Project project) {
        Set<String> files = new HashSet<>();

        // add custom routing files on settings
        List<RoutingFile> routingFiles = Settings.getInstance(project).routingFiles;
        if(routingFiles != null) {
            for (RoutingFile routingFile : routingFiles) {
                String path = routingFile.getPath();
                if(StringUtils.isNotBlank(path)) {
                    files.add(path);
                }
            }
        }

        // add defaults; if user never has changed the settings
        if(routingFiles == null || routingFiles.size() == 0) {
            Collections.addAll(files, Settings.DEFAULT_ROUTES);
        }

        for(String file: files) {

            File urlGeneratorFile = new File(getPath(project, file));
            VirtualFile virtualUrlGeneratorFile = VfsUtil.findFileByIoFile(urlGeneratorFile, false);
            if (virtualUrlGeneratorFile == null || !urlGeneratorFile.exists()) {

                // clean file cache
                if(COMPILED_CACHE.containsKey(project) && COMPILED_CACHE.get(project).containsKey(file)) {
                    COMPILED_CACHE.get(project).remove(file);
                }

            } else {

                if(!COMPILED_CACHE.containsKey(project)) {
                    COMPILED_CACHE.put(project, new HashMap<>());
                }

                Long routesLastModified = urlGeneratorFile.lastModified();
                if(!COMPILED_CACHE.get(project).containsKey(file) || !COMPILED_CACHE.get(project).get(file).getLastMod().equals(routesLastModified)) {

                    COMPILED_CACHE.get(project).put(file, new RoutesContainer(
                        routesLastModified,
                        RouteHelper.getRoutesInsideUrlGeneratorFile(project, virtualUrlGeneratorFile)
                    ));

                    Symfony2ProjectComponent.getLogger().info("update routing: " + urlGeneratorFile.toString());
                }
            }

        }

        Map<String, Route> routes = new HashMap<>();
        if(COMPILED_CACHE.containsKey(project)) {
            for (RoutesContainer container : COMPILED_CACHE.get(project).values()) {
                routes.putAll(container.getRoutes());
            }
        }

        RoutingLoaderParameter parameter = null;

        for (RoutingLoader routingLoader : ROUTING_LOADER.getExtensions()) {
            if(parameter == null) {
                parameter = new RoutingLoaderParameter(project, routes);
            }

            routingLoader.invoke(parameter);
        }

        return routes;
    }

    @NotNull
    public static Map<String, Route> getRoutesInsideUrlGeneratorFile(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        PsiFile psiFile = PsiElementUtils.virtualFileToPsiFile(project, virtualFile);
        if(!(psiFile instanceof PhpFile)) {
            return Collections.emptyMap();
        }

        return getRoutesInsideUrlGeneratorFile(psiFile);
    }


    /**
     * Temporary or remote files dont support "isInstanceOf", check for string implementation first
     */
    private static boolean isRouteClass(@NotNull PhpClass phpClass) {
        for (ClassReference classReference : phpClass.getExtendsList().getReferenceElements()) {
            String fqn = classReference.getFQN();
            if(fqn != null && StringUtils.stripStart(fqn, "\\").equalsIgnoreCase("Symfony\\Component\\Routing\\Generator\\UrlGenerator")) {
                return true;
            }
        }

        for (PhpClass phpInterface : phpClass.getImplementedInterfaces()) {
            String fqn = phpInterface.getFQN();
            if( StringUtils.stripStart(fqn, "\\").equalsIgnoreCase("Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface")) {
                return true;
            }
        }

        return PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");
    }

    @NotNull
    public static Map<String, Route> getRoutesInsideUrlGeneratorFile(@NotNull PsiFile psiFile) {

        Map<String, Route> routes = new HashMap<>();

        // heavy stuff here, to get nested routing array :)
        // list($variables, $defaults, $requirements, $tokens, $hostTokens)
        Collection<PhpClass> phpClasses = PsiTreeUtil.findChildrenOfType(psiFile, PhpClass.class);
        for(PhpClass phpClass: phpClasses) {
            if(!isRouteClass(phpClass)) {
                continue;
            }

            // Symfony < 2.8
            // static private $declaredRoutes = array(...)
            for(Field field: phpClass.getFields()) {
                if(!field.getName().equals("declaredRoutes")) {
                    continue;
                }

                PsiElement defaultValue = field.getDefaultValue();
                if(!(defaultValue instanceof ArrayCreationExpression)) {
                    continue;
                }

                collectRoutesOnArrayCreation(routes, (ArrayCreationExpression) defaultValue);
            }

            // Symfony >= 2.8
            // if (null === self::$declaredRoutes) {
            //   self::$declaredRoutes = array()
            // }
            Method constructor = phpClass.getConstructor();
            if(constructor == null) {
                continue;
            }

            for (FieldReference fieldReference : PsiTreeUtil.collectElementsOfType(constructor, FieldReference.class)) {
                String canonicalText = fieldReference.getCanonicalText();
                if(!"declaredRoutes".equals(canonicalText)) {
                    continue;
                }

                PsiElement assignExpression = fieldReference.getParent();
                if(!(assignExpression instanceof AssignmentExpression)) {
                    continue;
                }

                PhpPsiElement value = ((AssignmentExpression) assignExpression).getValue();
                if(!(value instanceof ArrayCreationExpression)) {
                    continue;
                }

                collectRoutesOnArrayCreation(routes, (ArrayCreationExpression) value);
            }
        }

        return routes;
    }

    /**
     * Collects routes in:
     *
     * array(
     *  _wdt' => array(..)
     * }
     *
     */
    private static void collectRoutesOnArrayCreation(@NotNull Map<String, Route> routes, @NotNull ArrayCreationExpression defaultValue) {
        for(ArrayHashElement arrayHashElement: defaultValue.getHashElements()) {
            PsiElement hashKey = arrayHashElement.getKey();
            if(!(hashKey instanceof StringLiteralExpression)) {
                continue;
            }

            String routeName = ((StringLiteralExpression) hashKey).getContents();
            if(!isProductionRouteName(routeName)) {
                continue;
            }

            routeName = convertLanguageRouteName(routeName);
            PsiElement hashValue = arrayHashElement.getValue();
            if(hashValue instanceof ArrayCreationExpression) {
                routes.put(routeName, convertRouteConfig(routeName, (ArrayCreationExpression) hashValue));
            }
        }
    }

    @NotNull
    private static Route convertRouteConfig(@NotNull String routeName, @NotNull ArrayCreationExpression hashValue) {
        List<ArrayHashElement> hashElementCollection = makeCollection(hashValue.getHashElements());

        Set<String> variables = new HashSet<>();
        if(hashElementCollection.size() >= 1 && hashElementCollection.get(0).getValue() instanceof ArrayCreationExpression) {
            ArrayCreationExpression value = (ArrayCreationExpression) hashElementCollection.get(0).getValue();
            if(value != null) {
                variables.addAll(PhpElementsUtil.getArrayKeyValueMap(value).values());
            }
        }

        Map<String, String> defaults = new HashMap<>();
        if(hashElementCollection.size() >= 2 && hashElementCollection.get(1).getValue() instanceof ArrayCreationExpression) {
            ArrayCreationExpression value = (ArrayCreationExpression) hashElementCollection.get(1).getValue();
            if(value != null) {
                defaults = PhpElementsUtil.getArrayKeyValueMap(value);
            }
        }

        Map<String, String>requirements = new HashMap<>();
        if(hashElementCollection.size() >= 3 && hashElementCollection.get(2).getValue() instanceof ArrayCreationExpression) {
            ArrayCreationExpression value = (ArrayCreationExpression) hashElementCollection.get(2).getValue();
            if(value != null) {
                requirements = PhpElementsUtil.getArrayKeyValueMap(value);
            }
        }

        List<Collection<String>> tokens = new ArrayList<>();
        if(hashElementCollection.size() >= 4 && hashElementCollection.get(3).getValue() instanceof ArrayCreationExpression) {
            ArrayCreationExpression tokenArray = (ArrayCreationExpression) hashElementCollection.get(3).getValue();
            if(tokenArray != null) {
                for(ArrayHashElement tokenArrayConfig: tokenArray.getHashElements()) {
                    if(tokenArrayConfig.getValue() instanceof ArrayCreationExpression) {
                        Map<String, String> arrayKeyValueMap = PhpElementsUtil.getArrayKeyValueMap((ArrayCreationExpression) tokenArrayConfig.getValue());
                        tokens.add(arrayKeyValueMap.values());
                    }
                }
            }

        }

        // hostTokens = 4 need them?
        return new Route(routeName, variables, defaults, requirements, tokens);
    }

    private static boolean isProductionRouteName(String routeName) {
        return !routeName.matches("_assetic_[0-9a-z]+[_\\d+]*");
    }

    /**
     * support I18nRoutingBundle
     */
    private static String convertLanguageRouteName(String routeName) {

        if(routeName.matches("^[a-z]{2}__RG__.*$")) {
            routeName = routeName.replaceAll("^[a-z]{2}+__RG__", "");
        }

        return routeName;
    }

    /**
     * Foo\Bar::methodAction
     */
    @Nullable
    private static String convertMethodToRouteControllerName(@NotNull Method method) {
        PhpClass phpClass = method.getContainingClass();
        if(phpClass == null) {
            return null;
        }

        return StringUtils.stripStart(phpClass.getFQN(), "\\") + "::" + method.getName();
    }

    /**
     * FooBundle:Bar::method
     * FooBundle:Bar\\Foo::method
     */
    @Nullable
    public static String convertMethodToRouteShortcutControllerName(@NotNull Method method) {
        PhpClass phpClass = method.getContainingClass();
        if(phpClass == null) {
            return null;
        }

        if("__invoke".equals(method.getName())) {
            return StringUtils.stripStart(phpClass.getFQN(), "\\");
        }

        String className = StringUtils.stripStart(phpClass.getFQN(), "\\");
        int bundlePos = className.lastIndexOf("Bundle\\");
        if(bundlePos == -1) {
            return null;
        }

        SymfonyBundle symfonyBundle = new SymfonyBundleUtil(method.getProject()).getContainingBundle(phpClass);
        if(symfonyBundle == null) {
            return null;
        }

        String methodName = method.getName();

        // strip method action => FoobarAction
        if(methodName.endsWith("Action")) {
            methodName = methodName.substring(0, methodName.length() - "Action".length());
        }

        // try to to find relative class name
        String controllerClass = className.toLowerCase();
        String bundleClass = StringUtils.stripStart(symfonyBundle.getNamespaceName(), "\\").toLowerCase();
        if(!controllerClass.startsWith(bundleClass)) {
            return null;
        }

        String relative = StringUtils.stripStart(phpClass.getFQN(), "\\").substring(bundleClass.length());
        if(relative.startsWith("Controller\\")) {
            relative = relative.substring("Controller\\".length());
        }

        if(relative.endsWith("Controller")) {
            relative = relative.substring(0, relative.length() - "Controller".length());
        }

        return String.format("%s:%s:%s", symfonyBundle.getName(), relative.replace("/", "\\"), methodName);
    }

    @NotNull
    private static Collection<VirtualFile> getRouteDefinitionInsideFile(@NotNull Project project, @NotNull String... routeNames) {

        Collection<VirtualFile> virtualFiles = new ArrayList<>();

        FileBasedIndex.getInstance().getFilesWithKey(RoutesStubIndex.KEY, new HashSet<>(Arrays.asList(routeNames)), virtualFile -> {
            virtualFiles.add(virtualFile);
            return true;
        }, GlobalSearchScope.allScope(project));

        return virtualFiles;

    }

    @NotNull
    public static Collection<StubIndexedRoute> getYamlRouteDefinitions(@NotNull YAMLDocument yamlDocument) {
        Collection<StubIndexedRoute> indexedRoutes = new ArrayList<>();

        for(YAMLKeyValue yamlKeyValue : YamlHelper.getTopLevelKeyValues((YAMLFile) yamlDocument.getContainingFile())) {

            YAMLValue element = yamlKeyValue.getValue();

            YAMLKeyValue path = YAMLUtil.findKeyInProbablyMapping(element, "path");

            // Symfony bc
            if(path == null) {
                path = YAMLUtil.findKeyInProbablyMapping(element, "pattern");
            }

            if(path == null) {
                continue;
            }

            // cleanup: 'foo', "foo"
            String keyText = StringUtils.strip(StringUtils.strip(yamlKeyValue.getKeyText(), "'"), "\"");
            if(StringUtils.isBlank(keyText)) {
                continue;
            }

            StubIndexedRoute route = new StubIndexedRoute(keyText);

            String routePath = path.getValueText();
            if(StringUtils.isNotBlank(routePath)) {
                route.setPath(routePath);
            }

            String methods = YamlHelper.getStringValueOfKeyInProbablyMapping(element, "methods");
            if(methods != null) {
                // value: [GET, POST,
                String[] split = methods.replace("[", "").replace("]", "").replaceAll(" +", "").toLowerCase().split(",");
                if(split.length > 0) {
                    route.addMethod(split);
                }
            }

            String controller = getYamlController(yamlKeyValue);
            if(controller != null) {
                route.setController(normalizeRouteController(controller));
            }

            indexedRoutes.add(route);
        }

        return indexedRoutes;

    }

    public static Collection<StubIndexedRoute> getXmlRouteDefinitions(XmlFile psiFile) {

        XmlDocumentImpl document = PsiTreeUtil.getChildOfType(psiFile, XmlDocumentImpl.class);
        if(document == null) {
            return Collections.emptyList();
        }

        Collection<StubIndexedRoute> indexedRoutes = new ArrayList<>();

        /*
         * <routes>
         *   <route id="foo" path="/blog/{slug}" methods="GET">
         *     <default key="_controller">Foo</default>
         *   </route>
         *
         *   <route id="foo" path="/blog/{slug}" methods="GET" controller="AppBundle:Blog:list"/>
         * </routes>
         */
        for(XmlTag xmlTag: PsiTreeUtil.getChildrenOfTypeAsList(psiFile.getFirstChild(), XmlTag.class)) {
            if(xmlTag.getName().equals("routes")) {
                for(XmlTag servicesTag: xmlTag.getSubTags()) {
                    if(servicesTag.getName().equals("route")) {
                        XmlAttribute xmlAttribute = servicesTag.getAttribute("id");
                        if(xmlAttribute != null) {
                            String attrValue = xmlAttribute.getValue();
                            if(attrValue != null && StringUtils.isNotBlank(attrValue)) {

                                StubIndexedRoute route = new StubIndexedRoute(attrValue);
                                String pathAttribute = servicesTag.getAttributeValue("path");
                                if(pathAttribute == null) {
                                    pathAttribute = servicesTag.getAttributeValue("pattern");
                                }

                                if(pathAttribute != null && StringUtils.isNotBlank(pathAttribute) ) {
                                    route.setPath(pathAttribute);
                                }

                                String methods = servicesTag.getAttributeValue("methods");
                                if(methods != null && StringUtils.isNotBlank(methods))  {
                                    String[] split = methods.replaceAll(" +", "").toLowerCase().split("\\|");
                                    if(split.length > 0) {
                                        route.addMethod(split);
                                    }
                                }

                                // <route><default key="_controller"/></route>
                                //  <route controller="AppBundle:Blog:list"/>
                                String controller = getXmlController(servicesTag);
                                if(controller != null) {
                                    route.setController(normalizeRouteController(controller));
                                }

                                indexedRoutes.add(route);
                            }
                        }
                    }
                }
            }
        }

        return indexedRoutes;
    }

    /**
     * <route controller="Foo"/>
     * <route>
     *     <default key="_controller">Foo</default>
     * </route>
     */
    @Nullable
    public static String getXmlController(@NotNull XmlTag serviceTag) {
        for(XmlTag subTag :serviceTag.getSubTags()) {
            if("default".equalsIgnoreCase(subTag.getName())) {
                String keyValue = subTag.getAttributeValue("key");
                if(keyValue != null && "_controller".equals(keyValue)) {
                    String actionName = subTag.getValue().getTrimmedText();
                    if(StringUtils.isNotBlank(actionName)) {
                        return actionName;
                    }
                }
            }
        }

        String controller = serviceTag.getAttributeValue("controller");
        if(controller != null && StringUtils.isNotBlank(controller)) {
            return controller;
        }

        return null;
    }

    /**
     * Find controller definition in yaml structure
     *
     * foo:
     *   defaults: { _controller: "Bundle:Foo:Bar" }
     *   defaults:
     *      _controller: "Bundle:Foo:Bar"
     *   controller: "Bundle:Foo:Bar"
     */
    @Nullable
    public static String getYamlController(@NotNull YAMLKeyValue psiElement) {
        YAMLKeyValue yamlKeyValue = YamlHelper.getYamlKeyValue(psiElement, "defaults");
        if(yamlKeyValue != null) {
            final YAMLValue container = yamlKeyValue.getValue();
            if(container instanceof YAMLMapping) {
                YAMLKeyValue yamlKeyValueController = YamlHelper.getYamlKeyValue(container, "_controller", true);
                if(yamlKeyValueController != null) {
                    String valueText = yamlKeyValueController.getValueText();
                    if(StringUtils.isNotBlank(valueText)) {
                        return valueText;
                    }
                }
            }
        }

        String controller = YamlHelper.getYamlKeyValueAsString(psiElement, "controller");
        if(controller != null && StringUtils.isNotBlank(controller)) {
            return controller;
        }

        return null;
    }

    @Nullable
    public static PsiElement getXmlRouteNameTarget(@NotNull XmlFile psiFile,@NotNull String routeName) {

        XmlDocumentImpl document = PsiTreeUtil.getChildOfType(psiFile, XmlDocumentImpl.class);
        if(document == null) {
            return null;
        }

        for(XmlTag xmlTag: PsiTreeUtil.getChildrenOfTypeAsList(psiFile.getFirstChild(), XmlTag.class)) {
            if(xmlTag.getName().equals("routes")) {
                for(XmlTag routeTag: xmlTag.getSubTags()) {
                    if(routeTag.getName().equals("route")) {
                        XmlAttribute xmlAttribute = routeTag.getAttribute("id");
                        if(xmlAttribute != null) {
                            String attrValue = xmlAttribute.getValue();
                            if(routeName.equals(attrValue)) {
                                return xmlAttribute;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    public static boolean isServiceController(@NotNull String shortcutName) {
        return !shortcutName.contains("::") && shortcutName.contains(":") && shortcutName.split(":").length == 2;
    }

    @NotNull
    public static List<Route> getRoutesOnControllerAction(@NotNull Method method) {
        Set<String> routeNames = new HashSet<>();

        ContainerUtil.addIfNotNull(routeNames, RouteHelper.convertMethodToRouteControllerName(method));
        ContainerUtil.addIfNotNull(routeNames, RouteHelper.convertMethodToRouteShortcutControllerName(method));

        Map<String, Route> allRoutes = getAllRoutes(method.getProject());
        List<Route> routes = new ArrayList<>();

        // resolve indexed routes
        if(routeNames.size() > 0) {
            routes.addAll(allRoutes.values().stream()
                .filter(route -> route.getController() != null && routeNames.contains(route.getController()))
                .collect(Collectors.toList())
            );
        }

        // search for services
        routes.addAll(
            ServiceRouteContainer.build(allRoutes).getMethodMatches(method)
        );

        return routes;
    }

    /**
     * Find every possible route name declaration inside yaml, xml or @Route annotation
     */
    @Nullable
    public static PsiElement getRouteNameTarget(@NotNull Project project, @NotNull String routeName) {
        for(VirtualFile virtualFile: RouteHelper.getRouteDefinitionInsideFile(project, routeName)) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);

            if(psiFile instanceof YAMLFile) {
                return YAMLUtil.getQualifiedKeyInFile((YAMLFile) psiFile, routeName);
            } else if(psiFile instanceof XmlFile) {
                PsiElement target = RouteHelper.getXmlRouteNameTarget((XmlFile) psiFile, routeName);
                if(target != null) {
                    return target;
                }
            } else if(psiFile instanceof PhpFile) {
                // find on @Route annotation
                for (PhpClass phpClass : PhpPsiUtil.findAllClasses((PhpFile) psiFile)) {
                    // get prefix by PhpClass
                    String prefix = getRouteNamePrefix(phpClass);

                    for (Method method : phpClass.getOwnMethods()) {
                        PhpDocComment docComment = method.getDocComment();
                        if(docComment == null) {
                            continue;
                        }

                        PhpDocCommentAnnotation container = AnnotationUtil.getPhpDocCommentAnnotationContainer(docComment);
                        if(container == null) {
                            continue;
                        }

                        // multiple @Route annotation in bundles are allowed
                        for (String routeClass : ROUTE_CLASSES) {
                            PhpDocTagAnnotation phpDocTagAnnotation = container.getPhpDocBlock(routeClass);
                            if(phpDocTagAnnotation != null) {
                                String annotationRouteName = phpDocTagAnnotation.getPropertyValue("name");
                                if(annotationRouteName != null) {
                                    // name provided @Route(name="foobar")
                                    if(routeName.equals(prefix + annotationRouteName)) {
                                        return phpDocTagAnnotation.getPropertyValuePsi("name");
                                    }
                                } else {
                                    // just @Route() without name provided
                                    String routeByMethod = AnnotationBackportUtil.getRouteByMethod(phpDocTagAnnotation.getPhpDocTag());
                                    if(routeName.equals(prefix + routeByMethod)) {
                                        return phpDocTagAnnotation.getPhpDocTag();
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
     * Extract route name of @Route(name="foobar_")
     * Must return empty string for easier accessibility
     */
    @NotNull
    private static String getRouteNamePrefix(@NotNull  PhpClass phpClass) {
        PhpDocCommentAnnotation phpClassContainer = AnnotationUtil.getPhpDocCommentAnnotationContainer(phpClass.getDocComment());
        if(phpClassContainer != null) {
            PhpDocTagAnnotation firstPhpDocBlock = phpClassContainer.getFirstPhpDocBlock(ROUTE_CLASSES.toArray(new String[ROUTE_CLASSES.size()]));
            if(firstPhpDocBlock != null) {
                String name = firstPhpDocBlock.getPropertyValue("name");
                if(name != null && StringUtils.isNotBlank(name)) {
                    return name;
                }
            }
        }

        return "";
    }

    @Nullable
    public static String getRouteUrl(Route route) {

        if(route.getPath() != null) {
            return route.getPath();
        }

        String url = "";

        // copy list;
        List<Collection<String>> tokens = new ArrayList<>(route.getTokens());
        Collections.reverse(tokens);

        for(Collection<String> token: tokens) {

            // copy, we are not allowed to mod list
            List<String> list = new ArrayList<>(token);

            if(list.size() >= 2 && list.get(1).equals("text")) {
                url = url.concat(list.get(0));
            }

            if(list.size() >= 4 && list.get(3).equals("variable")) {
                url = url.concat(list.get(2) + "{" + list.get(0) + "}");
            }

        }

        return url.length() == 0 ? null : url;
    }

    public static List<LookupElement> getRoutesLookupElements(final @NotNull Project project) {

        Map<String, Route> routes = RouteHelper.getCompiledRoutes(project);

        final List<LookupElement> lookupElements = new ArrayList<>();

        final Set<String> uniqueSet = new HashSet<>();
        for (Route route : routes.values()) {
            lookupElements.add(new RouteLookupElement(route));
            uniqueSet.add(route.getName());
        }

        for(String routeName: SymfonyProcessors.createResult(project, RoutesStubIndex.KEY, uniqueSet)) {
            if(uniqueSet.contains(routeName)) {
                continue;
            }

            for(StubIndexedRoute route: FileBasedIndex.getInstance().getValues(RoutesStubIndex.KEY, routeName, GlobalSearchScope.allScope(project))) {
                lookupElements.add(new RouteLookupElement(new Route(route), true));
                uniqueSet.add(routeName);
            }
        }

        return lookupElements;

    }

    @NotNull
    public static List<PsiElement> getRouteDefinitionTargets(Project project, String routeName) {

        List<PsiElement> targets = new ArrayList<>();
        Collections.addAll(targets, RouteHelper.getMethods(project, routeName));

        PsiElement yamlKey = RouteHelper.getRouteNameTarget(project, routeName);
        if(yamlKey != null) {
            targets.add(yamlKey);
        }

        return targets;
    }

    @NotNull
    synchronized public static Map<String, Route> getAllRoutes(final @NotNull Project project) {
        CachedValue<Map<String, Route>> cache = project.getUserData(ROUTE_CACHE);

        if (cache == null) {
            cache = CachedValuesManager.getManager(project).createCachedValue(() ->
                CachedValueProvider.Result.create(getAllRoutesProxy(project), PsiModificationTracker.MODIFICATION_COUNT),
                false
            );
            project.putUserData(ROUTE_CACHE, cache);
        }

        return cache.getValue();
    }

    @NotNull
    private static Map<String, Route> getAllRoutesProxy(@NotNull Project project) {

        Map<String, Route> routes = new HashMap<>();
        routes.putAll(RouteHelper.getCompiledRoutes(project));

        Set<String> uniqueKeySet = new HashSet<>(routes.keySet());

        for(String routeName: SymfonyProcessors.createResult(project, RoutesStubIndex.KEY, uniqueKeySet)) {
            if(uniqueKeySet.contains(routeName)) {
                continue;
            }

            for(StubIndexedRoute route: FileBasedIndex.getInstance().getValues(RoutesStubIndex.KEY, routeName, GlobalSearchScope.allScope(project))) {
                uniqueKeySet.add(routeName);
                routes.put(routeName, new Route(route));
            }
        }

        return routes;
    }

    /**
     * Foobar/Bar => Foobar\Bar
     * \\Foobar\Foobar => Foobar\Bar
     */
    @NotNull
    private static String normalizeRouteController(@NotNull String string) {
        string = string.replace("/", "\\");
        string = StringUtils.stripStart(string,"\\");

        return string;
    }

    /**
     * Support "use Symfony\Component\Routing\Annotation\Route as BaseRoute;"
     */
    public static boolean isRouteClassAnnotation(@NotNull String clazz) {
        String myClazz = StringUtils.stripStart(clazz, "\\");
        return ROUTE_CLASSES.stream().anyMatch(s -> s.equalsIgnoreCase(myClazz));
    }
}
