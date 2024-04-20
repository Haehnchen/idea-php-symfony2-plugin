package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SimpleModificationTracker;
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
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.refactoring.PhpNameUtil;
import de.espend.idea.php.annotation.dict.PhpDocCommentAnnotation;
import de.espend.idea.php.annotation.dict.PhpDocTagAnnotation;
import de.espend.idea.php.annotation.util.AnnotationUtil;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.extension.RoutingLoader;
import fr.adrienbrault.idea.symfony2plugin.extension.RoutingLoaderParameter;
import fr.adrienbrault.idea.symfony2plugin.routing.dic.ControllerClassOnShortcutReturn;
import fr.adrienbrault.idea.symfony2plugin.routing.dic.ServiceRouteContainer;
import fr.adrienbrault.idea.symfony2plugin.routing.dict.RoutingFile;
import fr.adrienbrault.idea.symfony2plugin.stubs.SymfonyProcessors;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.StubIndexedRoute;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.RoutesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.visitor.AnnotationRouteElementVisitor;
import fr.adrienbrault.idea.symfony2plugin.ui.dict.AbstractUiFilePath;
import fr.adrienbrault.idea.symfony2plugin.util.*;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerAction;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import kotlin.Pair;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.*;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteHelper {
    public static final String[] ROUTE_ANNOTATIONS = new String[] {
        "\\Symfony\\Component\\Routing\\Annotation\\Route",
        "\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Route",
        "\\Symfony\\Component\\Routing\\Attribute\\Route"
    };

    private static final Key<CachedValue<Map<String, Route>>> ROUTE_CACHE = new Key<>("SYMFONY:ROUTE_CACHE");
    private static final Key<CachedValue<Set<String>>> ROUTE_CONTROLLER_RESOLVED_CACHE = new Key<>("ROUTE_CONTROLLER_RESOLVED_CACHE");

    private static final Key<CachedValue<Map<String, Route>>> SYMFONY_COMPILED_CACHE_ROUTES = new Key<>("SYMFONY_COMPILED_CACHE_ROUTES");
    private static final Key<CachedValue<Collection<String>>> SYMFONY_COMPILED_CACHE_ROUTES_FILES = new Key<>("SYMFONY_COMPILED_CACHE_ROUTES_FILES");
    private static final Key<CachedValue<Collection<String>>> SYMFONY_COMPILED_GUESTED_FILES = new Key<>("SYMFONY_COMPILED_GUESTED_FILES");

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

        return lookupElements.toArray(new LookupElement[0]);
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

    public static PsiElement[] getRouteParameterPsiElements(@NotNull Project project, @NotNull String routeName, @NotNull String parameterName) {
        Collection<PsiElement> results = new ArrayList<>();

        for (PsiElement psiElement : RouteHelper.getMethods(project, routeName)) {
            if (psiElement instanceof Method method) {
                Parameter parameter = method.getParameter(parameterName);
                if (parameter != null) {
                    results.add(parameter);
                }
            }
        }

        return results.toArray(new PsiElement[0]);
    }

    @NotNull
    public static PsiElement[] getMethods(@NotNull Project project, @NotNull String routeName) {
        Set<PsiElement> targets = new HashSet<>();

        for (Route route : getRoute(project, routeName)) {
            targets.addAll(Arrays.asList(getMethodsOnControllerShortcut(project, route.getController())));
        }

        return targets.toArray(new PsiElement[0]);
    }

    public static Collection<Route> findRoutesByPath(@NotNull Project project, @NotNull String path) {
        return RouteHelper.getAllRoutes(project)
            .values()
            .stream()
            .filter(route -> path.equals(route.getPath()))
            .collect(Collectors.toList());
    }

    /**
     * Reverse routing matching, find any "incomplete" string inside the route pattern
     *
     * - "foo/bar" => "/foo/bar"
     * - "foo/12" => "/foo/{edit}"
     * - "ar/12/foo" => "/car/{edit}/foobar"
     */
    @NotNull
    public static Collection<Pair<Route, PsiElement>> getMethodsForPathWithPlaceholderMatchRoutes(@NotNull Project project, @NotNull String searchPath) {
        Collection<Pair<Route, PsiElement>> targets = new ArrayList<>();

        for (Route route : getRoutesForPathWithPlaceholderMatch(project, searchPath)) {
            List<Pair<Route, PsiElement>> list = Arrays.stream(getMethodsOnControllerShortcut(project, route.getController()))
                .map(psiElement -> new Pair<>(route, psiElement))
                .toList();

            targets.addAll(list);
        }

        return targets;
    }

    /**
     * Reverse routing matching, find any "incomplete" string inside the route pattern
     *
     * - "foo/bar" => "/foo/bar"
     * - "foo/12" => "/foo/{edit}"
     * - "ar/12/foo" => "/car/{edit}/foobar"
     */
    @NotNull
    public static PsiElement[] getMethodsForPathWithPlaceholderMatch(@NotNull Project project, @NotNull String searchPath) {
        Collection<PsiElement> psiElements = new HashSet<>();
        for (Pair<Route, PsiElement> entry : RouteHelper.getMethodsForPathWithPlaceholderMatchRoutes(project, searchPath)) {
            psiElements.add(entry.getSecond());
        }

        return psiElements.toArray(new PsiElement[0]);
    }

    /**
     * Reverse routing matching, find any "incomplete" string inside the route pattern
     *
     * - "foo/bar" => "/foo/bar"
     * - "foo/12" => "/foo/{edit}"
     * - "ar/12/foo" => "/car/{edit}/foobar"
     */
    @NotNull
    public static Collection<Route> getRoutesForPathWithPlaceholderMatch(@NotNull Project project, @NotNull String searchPath) {
        return new ArrayList<>(RouteHelper.getAllRoutes(project).values())
            .parallelStream()
            .filter(Objects::nonNull)
            .map(route -> {
                if (isReverseRoutePatternMatch(route, searchPath)) {
                    return route;
                }
                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Reverse routing matching, find any "incomplete" string inside the route pattern
     *
     * - "foo/bar" => "/foo/bar"
     * - "foo/12" => "/foo/{edit}"
     * - "ar/12/foo" => "/car/{edit}/foobar"
     */
    public static boolean hasRoutesForPathWithPlaceholderMatch(@NotNull Project project, @NotNull String searchPath) {
        return RouteHelper.getAllRoutes(project).values()
            .parallelStream()
            .anyMatch(route -> isReverseRoutePatternMatch(route, searchPath));
    }

    private static boolean isReverseRoutePatternMatch(@NotNull Route route, @NotNull String searchPath) {
        String routePath = route.getPath();
        if (routePath == null) {
            return false;
        }

        if (routePath.contains(searchPath)) {
            return true;
        }

        // String string = "|"; visibility debug
        String string = Character.toString((char) 156);

        String routePathPlaceholderNeutral = routePath.replaceAll("\\{([^}]*)}", string);
        String match = null;
        int startIndex = -1;

        // find first common non pattern string, string on at 2 for no fetching all; right to left
        for (int i = 2; i < searchPath.length(); i++) {
            String text = searchPath.substring(0, searchPath.length() - i);

            int i1 = routePathPlaceholderNeutral.indexOf(text);
            if (i1 >= 0) {
                match = routePathPlaceholderNeutral.substring(i1);
                startIndex = text.length();
                break;
            }
        }

        if (match == null) {
            return false;
        }

        // find a pattern match: left to right
        int endIndex = match.length();
        for (int i = startIndex + 1; i <= endIndex; i++) {
            String substring = match.substring(0, i);

            String regex = substring.replace(string, "[\\w-]+");

            // user input
            try {
                Matcher matcher = Pattern.compile(regex).matcher(searchPath);
                if (matcher.matches()) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        return false;
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

        // escaping
        // "Foobar\\Test"
        controllerName = controllerName.replace("\\\\", "\\");

        if(controllerName.contains("::")) {
            // FooBundle\Controller\BarController::fooBarAction
            String className = controllerName.substring(0, controllerName.lastIndexOf("::"));
            String methodName = controllerName.substring(controllerName.lastIndexOf("::") + 2);

            Method method = PhpElementsUtil.getClassMethod(project, className, methodName);

            Collection<Method> methods = new HashSet<>();
            if (method != null) {
                methods.add(method);
            }

            if (!methodName.toLowerCase().endsWith("action")) {
                Method methodAction = PhpElementsUtil.getClassMethod(project, className, methodName + "Action");
                if (methodAction != null) {
                    methods.add(methodAction);
                }
            }

            // web_profiler.controller.profiler::homeAction
            // foo_service_bar::fooBar
            ControllerAction controllerServiceAction = new ControllerIndex(project).getControllerActionOnService(controllerName);
            if(controllerServiceAction != null) {
                methods.add(controllerServiceAction.getMethod());
            }

            return methods.toArray(new PsiElement[0]);
        } else if(controllerName.contains(":")) {
            // AcmeDemoBundle:Demo:hello
            String[] split = controllerName.split(":");
            if(split.length == 3) {
                Collection<Method> controllerMethod = ControllerIndex.getControllerMethod(project, controllerName);
                if(!controllerMethod.isEmpty()) {
                    return controllerMethod.toArray(new PsiElement[0]);
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
            return phpClass.toArray(new PsiElement[0]);
        } else {
            // foo_service_bar
            ControllerAction controllerServiceAction = new ControllerIndex(project).getControllerActionOnService(controllerName);
            if(controllerServiceAction != null) {
                return new PsiElement[] {controllerServiceAction.getMethod()};
            }
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
    public static ControllerClassOnShortcutReturn getControllerClassOnShortcut(@NotNull Project project, @NotNull String controllerName) {
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

    private static String getPath(Project project, String path) {
        if (!FileUtil.isAbsolute(path)) { // Project relative path
            path = project.getBasePath() + "/" + path;
        }

        return path;
    }

    @NotNull
    private static Set<String> getDefaultRoutes(@NotNull Project project) {
        Set<String> allFiles = new HashSet<>();

        VirtualFile projectDir = ProjectUtil.getProjectDir(project);
        if (projectDir != null) {
            VirtualFile varCache = VfsUtil.findRelativeFile(projectDir, "var", "cache");
            if (varCache != null) {
                String path1 = varCache.getPath();

                Collection<String> cachedValue = CachedValuesManager.getManager(project).getCachedValue(
                    project,
                    SYMFONY_COMPILED_GUESTED_FILES,
                    () -> {
                        Set<String> files = new HashSet<>();

                        // old "app/cache" is ignored for now
                        VirtualFile cache = VfsUtil.findRelativeFile(projectDir, "var", "cache");
                        for (VirtualFile child : cache != null ? cache.getChildren() : new VirtualFile[] {}) {
                            String filename = child.getName();
                            // support "dev" and "dev_*"
                            if ("dev".equals(filename) || filename.startsWith("dev_")) {
                                for (VirtualFile childChild : child.getChildren()) {
                                    if (childChild.isDirectory() || !"php".equalsIgnoreCase(childChild.getExtension())) {
                                        continue;
                                    }

                                    // guess a compiled php file by its normalized name
                                    // some common examples, from Symfony 2 to now :)
                                    // appDevDebugProjectContainerUrlGenerator, UrlGenerator
                                    String s = childChild.getNameWithoutExtension().toLowerCase().replace("_", "");
                                    if (s.contains("urlgenerator") || s.contains("urlgenerating")) {
                                        String path = VfsUtil.getRelativePath(childChild, projectDir, '/');
                                        if (path != null) {
                                            files.add(path.replace("\\", "//"));
                                        }
                                    }
                                }
                            }
                        }

                        return CachedValueProvider.Result.create(Collections.unmodifiableSet(files), new AbsoluteFileModificationTracker(List.of(path1)));
                    },
                    false
                );

                allFiles.addAll(cachedValue);
            }
        }

        // work with cloned data
        Set<String> files2 = new HashSet<>(allFiles);
        files2.addAll(Settings.DEFAULT_ROUTES);
        return files2;
    }

    @NotNull
    private static synchronized Collection<String> getCompiledRouteFiles(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            SYMFONY_COMPILED_CACHE_ROUTES_FILES,
            () -> {
                Set<String> files = new HashSet<>();

                // add custom routing files on settings
                List<RoutingFile> routingFiles = Settings.getInstance(project).routingFiles;
                if (routingFiles != null) {
                    files = routingFiles.stream().map(AbstractUiFilePath::getPath)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toSet());
                }

                files.addAll(getDefaultRoutes(project));

                Set<String> filesAbsolute = files.stream()
                    .map(s -> getPath(project, s))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

                return CachedValueProvider.Result.create(Collections.unmodifiableSet(filesAbsolute), PsiModificationTracker.MODIFICATION_COUNT);
            },
            false
        );
    }

    @NotNull
    private static Map<String, Route> getCompiledRoutes(@NotNull Project project) {
        Map<String, Route> routesCache = CachedValuesManager.getManager(project).getCachedValue(
            project,
            SYMFONY_COMPILED_CACHE_ROUTES,
            () -> {
                Map<String, Route> routesContainerMap = new HashMap<>();

                Collection<String> compiledRouteFiles = getCompiledRouteFiles(project);

                for (String file : compiledRouteFiles) {
                    File urlGeneratorFile = new File(getPath(project, file));
                    VirtualFile virtualUrlGeneratorFile = VfsUtil.findFileByIoFile(urlGeneratorFile, false);
                    if (virtualUrlGeneratorFile == null || !urlGeneratorFile.exists()) {
                        continue;
                    }

                    routesContainerMap.putAll(RouteHelper.getRoutesInsideUrlGeneratorFile(project, virtualUrlGeneratorFile));
                }

                return CachedValueProvider.Result.create(
                    Collections.unmodifiableMap(routesContainerMap),
                    new CompiledRoutePathFilesModificationTracker(project),
                    new AbsoluteFileModificationTracker(compiledRouteFiles)
                );
            },
            false
        );

        Map<String, Route> routes = new HashMap<>(routesCache);
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

        // Symfony >= 4
        // extract the routes on a return statement
        // return [['route'] => [...]]
        for (PhpReturn phpReturn : PsiTreeUtil.findChildrenOfType(psiFile, PhpReturn.class)) {
            PsiElement argument = phpReturn.getArgument();
            if (!(argument instanceof ArrayCreationExpression)) {
                continue;
            }

            // get only the inside arrays
            // [[..], [..]] => [..], [..]
            for (Map.Entry<String, PsiElement> routeArray : PhpElementsUtil.getArrayKeyValueMapWithValueAsPsiElement((ArrayCreationExpression) argument).entrySet()) {
                List<ArrayCreationExpression> routeArrayOptions = new ArrayList<>();
                for (PhpPsiElement routeOption : PsiTreeUtil.getChildrenOfTypeAsList(routeArray.getValue(), PhpPsiElement.class)) {
                    routeArrayOptions.add(PsiTreeUtil.getChildOfType(routeOption, ArrayCreationExpression.class));
                }

                Route route = convertRouteConfigForReturnArray(routeArray.getKey(), routeArrayOptions);
                routes.put(routeArray.getKey(), route);

                for (ArrayCreationExpression expression : routeArrayOptions) {
                    for (ArrayHashElement arrayHashElement : expression.getHashElements()) {
                        if (arrayHashElement.getKey() instanceof StringLiteralExpression stringLiteralExpression && "_canonical_route".equals(stringLiteralExpression.getContents())) {
                            if (arrayHashElement.getValue() instanceof StringLiteralExpression literalExpression) {
                                String canonical = literalExpression.getContents();
                                if (!canonical.isBlank() && !routes.containsKey(canonical)) {
                                    Route routeCanonical = convertRouteConfigForReturnArray(canonical, routeArrayOptions);
                                    routes.put(canonical, routeCanonical);
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }

        // Symfony < 4
        // heavy stuff here, to get nested routing array :)
        // list($variables, $defaults, $requirements, $tokens, $hostTokens)
        Collection<PhpClass> phpClasses = PsiTreeUtil.findChildrenOfType(psiFile, PhpClass.class);
        for(PhpClass phpClass: phpClasses) {
            if(!isRouteClass(phpClass)) {
                continue;
            }

            // Symfony < 2.8
            // static private $declaredRoutes = array(...)
            // only "getOwnFields" is uncached and dont breaks; find* methods are cached resulting in exceptions
            Field[] ownFields = phpClass.getOwnFields();
            for (Field ownField : ownFields) {
                if ("declaredRoutes".equals(ownField.getName())) {
                    PsiElement defaultValue = ownField.getDefaultValue();
                    if(!(defaultValue instanceof ArrayCreationExpression)) {
                        continue;
                    }

                    collectRoutesOnArrayCreation(routes, (ArrayCreationExpression) defaultValue);
                }
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

    /**
     * Used in Symfony > 4 where routes are wrapped into a return array
     */
    @NotNull
    private static Route convertRouteConfigForReturnArray(@NotNull String routeName, @NotNull List<ArrayCreationExpression> hashElementCollection) {
        Set<String> variables = new HashSet<>();
        if(!hashElementCollection.isEmpty() && hashElementCollection.get(0) != null) {
            ArrayCreationExpression value = hashElementCollection.get(0);
            if(value != null) {
                variables.addAll(PhpElementsUtil.getArrayValuesAsString(value));
            }
        }

        Map<String, String> defaults = new HashMap<>();
        if(hashElementCollection.size() >= 2 && hashElementCollection.get(1) != null) {
            ArrayCreationExpression value = hashElementCollection.get(1);
            if(value != null) {
                defaults = PhpElementsUtil.getArrayKeyValueMap(value);
            }
        }

        Map<String, String>requirements = new HashMap<>();
        if(hashElementCollection.size() >= 3 && hashElementCollection.get(2) != null) {
            ArrayCreationExpression value = hashElementCollection.get(2);
            if(value != null) {
                requirements = PhpElementsUtil.getArrayKeyValueMap(value);
            }
        }

        StringBuilder path = new StringBuilder();
        List<Collection<String>> tokens = new ArrayList<>();
        if(hashElementCollection.size() >= 4 && hashElementCollection.get(3) != null) {
            ArrayCreationExpression tokenArray = hashElementCollection.get(3);
            if(tokenArray != null) {
                List<PsiElement> urlParts = PhpPsiUtil.getChildren(tokenArray, psiElement ->
                    psiElement.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE
                );

                Collections.reverse(urlParts);

                for(PsiElement tokenArrayConfig: urlParts) {
                    PsiElement firstChild = tokenArrayConfig.getFirstChild();
                    if(firstChild instanceof ArrayCreationExpression) {
                        List<PsiElement> foo = PhpPsiUtil.getChildren(firstChild, psiElement ->
                            psiElement.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE
                        );

                        List<String> collect = foo.stream()
                            .map(psiElement -> psiElement.getFirstChild() instanceof StringLiteralExpression ? ((StringLiteralExpression) psiElement.getFirstChild()).getContents() : null)
                            .toList();

                        if (!collect.isEmpty()) {
                            path.append(collect.get(1));
                        }

                        if (collect.size() > 2) {
                            String var = collect.get(3);
                            if (var != null) {
                                path.append("{").append(var).append("}");
                            }
                        }
                    }
                }
            }

        }

        // hostTokens = 4 need them?
        return new Route(routeName, variables, defaults, requirements, tokens, (path.isEmpty()) ? null : path.toString());
    }

    /**
     * Used in Symfony < 4 where routes are wrapped into a class
     */
    @NotNull
    private static Route convertRouteConfig(@NotNull String routeName, @NotNull ArrayCreationExpression hashValue) {
        List<ArrayHashElement> hashElementCollection = new ArrayList<>();
        hashValue.getHashElements().forEach(hashElementCollection::add);

        Set<String> variables = new HashSet<>();
        if(!hashElementCollection.isEmpty() && hashElementCollection.get(0).getValue() instanceof ArrayCreationExpression value) {
            if(value != null) {
                variables.addAll(PhpElementsUtil.getArrayKeyValueMap(value).values());
            }
        }

        Map<String, String> defaults = new HashMap<>();
        if(hashElementCollection.size() >= 2 && hashElementCollection.get(1).getValue() instanceof ArrayCreationExpression value) {
            if(value != null) {
                defaults = PhpElementsUtil.getArrayKeyValueMap(value);
            }
        }

        Map<String, String>requirements = new HashMap<>();
        if(hashElementCollection.size() >= 3 && hashElementCollection.get(2).getValue() instanceof ArrayCreationExpression value) {
            if(value != null) {
                requirements = PhpElementsUtil.getArrayKeyValueMap(value);
            }
        }

        StringBuilder path = new StringBuilder();
        List<Collection<String>> tokens = new ArrayList<>();
        if(hashElementCollection.size() >= 4 && hashElementCollection.get(3).getValue() instanceof ArrayCreationExpression tokenArray) {
            if(tokenArray != null) {
                List<ArrayHashElement> result = StreamSupport.stream(tokenArray.getHashElements().spliterator(), false)
                        .collect(Collectors.toList());

                Collections.reverse(result);

                for(ArrayHashElement tokenArrayConfig: result) {
                    if(tokenArrayConfig.getValue() instanceof ArrayCreationExpression) {
                        Map<String, String> arrayKeyValueMap = PhpElementsUtil.getArrayKeyValueMap((ArrayCreationExpression) tokenArrayConfig.getValue());
                        path.append(arrayKeyValueMap.getOrDefault("1", null));

                        String var = arrayKeyValueMap.getOrDefault("3", null);
                        if (var != null) {
                            path.append("{").append(var).append("}");
                        }

                        tokens.add(arrayKeyValueMap.values());
                    }
                }
            }

        }

        // hostTokens = 4 need them?
        return new Route(routeName, variables, defaults, requirements, tokens, (path.isEmpty()) ? null : path.toString());
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

    /**
     * Find controller definition in php function call.
     * $routes->controller('FooController:method');
     * $routes->controller([FooController::class, 'method']);
     * $routes->controller(FooController::class);
     */
    @NotNull
    public static PsiElement[] getPhpController(@NotNull MethodReference methodCall) {
        PsiElement[] parameters = methodCall.getParameters();
        if (parameters.length == 1 && parameters[0] instanceof StringLiteralExpression) {
            // 'FooController::method'
            String contents = ((StringLiteralExpression) parameters[0]).getContents();
            if (StringUtils.isNotBlank(contents)) {
                return RouteHelper.getMethodsOnControllerShortcut(methodCall.getProject(), contents);
            }

            return new PsiElement[0];
        } else if (parameters.length == 1 && parameters[0] instanceof ClassConstantReference) {
            // FooController::class
            String classConstantPhpFqn = PhpElementsUtil.getClassConstantPhpFqn((ClassConstantReference) parameters[0]);
            if (StringUtils.isNotBlank(classConstantPhpFqn)) {
                return RouteHelper.getMethodsOnControllerShortcut(methodCall.getProject(), classConstantPhpFqn);
            }

            return new PsiElement[0];
        } else if (parameters.length == 1 && parameters[0] instanceof ArrayCreationExpression) {
            // [FooController::class, 'method']
            PsiElement[] elements = PhpElementsUtil.getArrayValues((ArrayCreationExpression) parameters[0]);
            if (elements.length == 2) {
                String className = PhpElementsUtil.getStringValue(elements[0]);
                if (StringUtils.isNotBlank(className)) {
                    String method = PhpElementsUtil.getStringValue(elements[1]);
                    if (method != null) {
                        return RouteHelper.getMethodsOnControllerShortcut(methodCall.getProject(), className + "::" + method);
                    }
                }
            }

            return new PsiElement[0];
        }

        return new PsiElement[0];
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
        return shortcutName.contains(":") && shortcutName.replace("::", ":").split(":").length == 2;
    }

    public static boolean isServiceControllerInvoke(@NotNull String shortcutName) {
        return !shortcutName.contains(":");
    }

    @NotNull
    public static List<Route> getRoutesOnControllerAction(@NotNull Method method) {
        Set<String> routeNames = new HashSet<>();

        ContainerUtil.addIfNotNull(routeNames, RouteHelper.convertMethodToRouteControllerName(method));
        ContainerUtil.addIfNotNull(routeNames, RouteHelper.convertMethodToRouteShortcutControllerName(method));

        Project project = method.getProject();
        Map<String, Route> allRoutes = getAllRoutes(project);
        List<Route> routes = new ArrayList<>();

        // resolve indexed routes
        if(!routeNames.isEmpty()) {
            routes.addAll(allRoutes.values().stream()
                .filter(route -> route.getController() != null && routeNames.contains(route.getController()))
                .toList()
            );
        }

        // search for services
        routes.addAll(ServiceRouteContainer.build(project, allRoutes).getMethodMatches(method));

        return routes;
    }

    /**
     * Find every possible route name declaration inside yaml, xml or @Route annotation
     */
    @NotNull
    public static Collection<PsiElement> getRouteNameTarget(@NotNull Project project, @NotNull String routeName) {
        for(VirtualFile virtualFile: RouteHelper.getRouteDefinitionInsideFile(project, routeName)) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);

            if(psiFile instanceof YAMLFile) {
                YAMLKeyValue qualifiedKeyInFile = YAMLUtil.getQualifiedKeyInFile((YAMLFile) psiFile, routeName);
                if (qualifiedKeyInFile != null) {
                    return Collections.singletonList(qualifiedKeyInFile);
                }

                return Collections.emptyList();
            } else if(psiFile instanceof XmlFile) {
                PsiElement target = RouteHelper.getXmlRouteNameTarget((XmlFile) psiFile, routeName);
                if(target != null) {
                    return Collections.singletonList(target);
                }
            } else if(psiFile instanceof PhpFile) {
                Collection<PsiElement> targets = new ArrayList<>();

                for (PhpClass phpClass : PhpPsiUtil.findAllClasses((PhpFile) psiFile)) {
                    new AnnotationRouteElementVisitor(pair -> {
                        if (routeName.equalsIgnoreCase(pair.getFirst())) {
                            targets.add(pair.getSecond());
                        }
                    }).visitFile(phpClass);
                }

                return targets;
            }
        }

        return Collections.emptyList();
    }

    /**
     * Extract route name of @Route(name="foobar_")
     * Must return empty string for easier accessibility
     */
    @NotNull
    private static String getRouteNamePrefix(@NotNull  PhpClass phpClass) {
        PhpDocCommentAnnotation phpClassContainer = AnnotationUtil.getPhpDocCommentAnnotationContainer(phpClass.getDocComment());
        if(phpClassContainer != null) {
            PhpDocTagAnnotation firstPhpDocBlock = phpClassContainer.getFirstPhpDocBlock(ROUTE_ANNOTATIONS);
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

        return url.isEmpty() ? null : url;
    }

    public static List<LookupElement> getRoutesLookupElements(final @NotNull Project project) {

        Map<String, Route> routes = RouteHelper.getCompiledRoutes(project);

        final List<LookupElement> lookupElements = new ArrayList<>();

        final Set<String> uniqueSet = new HashSet<>();
        for (Route route : routes.values()) {
            lookupElements.add(new RouteLookupElement(route));
            uniqueSet.add(route.getName());
        }

        for(String routeName: SymfonyProcessors.createResult(project, RoutesStubIndex.KEY)) {
            if (uniqueSet.contains(routeName)) {
                continue;
            }

            for (StubIndexedRoute route: FileBasedIndex.getInstance().getValues(RoutesStubIndex.KEY, routeName, GlobalSearchScope.allScope(project))) {
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
        targets.addAll(RouteHelper.getRouteNameTarget(project, routeName));
        return targets;
    }

    public static boolean isRouteExistingForMethod(final @NotNull Method method) {
        Project project = method.getProject();

        Set<String> cachedValue = CachedValuesManager.getManager(project).getCachedValue(
            project,
            ROUTE_CONTROLLER_RESOLVED_CACHE,
            () -> {
                Set<String> items = new HashSet<>();

                for (Map.Entry<String, Route> pair : RouteHelper.getAllRoutes(project).entrySet()) {
                    String controller = pair.getValue().getController();
                    if (controller != null) {
                        for (PsiElement psiElement : RouteHelper.getMethodsOnControllerShortcut(project, controller)) {
                            if (psiElement instanceof Method) {
                                items.add(((Method) psiElement).getFQN());
                            }
                        }
                    }
                }

                return CachedValueProvider.Result.create(items, PsiModificationTracker.MODIFICATION_COUNT);
            },
            false
        );

        String fqn = method.getFQN();
        if (fqn.toLowerCase().endsWith("action")) {
            String substring = fqn.substring(0, fqn .length() - "action".length());
            if (cachedValue.contains(substring)) {
                return true;
            }
        }

        return cachedValue.contains(fqn);
    }

    @NotNull
    public static Map<String, Route> getAllRoutes(final @NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            ROUTE_CACHE,
            () -> {
                Map<String, Route> routes = new HashMap<>(RouteHelper.getCompiledRoutes(project));
                Set<String> uniqueKeySet = new HashSet<>(routes.keySet());

                for (String routeName: SymfonyProcessors.createResult(project, RoutesStubIndex.KEY)) {
                    if (uniqueKeySet.contains(routeName)) {
                        continue;
                    }

                    for (StubIndexedRoute route: FileBasedIndex.getInstance().getValues(RoutesStubIndex.KEY, routeName, GlobalSearchScope.allScope(project))) {
                        uniqueKeySet.add(routeName);
                        routes.put(routeName, new Route(route));
                    }
                }

                return CachedValueProvider.Result.create(
                    Collections.unmodifiableMap(routes),
                    FileIndexCaches.getModificationTrackerForIndexId(project, RoutesStubIndex.KEY), // index
                    new CompiledRoutePathFilesModificationTracker(project), // compiled
                    new AbsoluteFileModificationTracker(getCompiledRouteFiles(project)) // compiled
                );
            },
            false
        );
    }

    @NotNull
    public static Collection<LookupElement> getRoutesPathLookupElements(final @NotNull Project project) {
        Collection<LookupElement> lookupElements = new ArrayList<>();

        for (Route route : RouteHelper.getAllRoutes(project).values()) {
            String path = route.getPath();
            if (path != null && !route.getName().startsWith("_")) {
                LookupElementBuilder element = LookupElementBuilder.create(path)
                    .withTypeText(route.getName())
                    .withIcon(Symfony2Icons.ROUTE_WEAK);

                lookupElements.add(element);
            }
        }

        return lookupElements;
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
        String myClazz = "\\" + StringUtils.stripStart(clazz, "\\");
        return Arrays.stream(ROUTE_ANNOTATIONS).anyMatch(s -> s.equalsIgnoreCase(myClazz));
    }

    private static class CompiledRoutePathFilesModificationTracker extends SimpleModificationTracker {
        private final @NotNull Project project;
        private int last = 0;

        public CompiledRoutePathFilesModificationTracker(@NotNull Project project) {
            this.project = project;
        }

        @Override
        public long getModificationCount() {
            int hash = getCompiledRouteFiles(project).stream().sorted().collect(Collectors.joining()).hashCode();
            if (hash != this.last) {
                this.last = hash;
                this.incModificationCount();
            }

            return super.getModificationCount();
        }
    }
}
