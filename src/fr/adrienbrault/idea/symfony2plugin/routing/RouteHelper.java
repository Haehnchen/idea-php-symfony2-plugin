package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.xml.XmlDocumentImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.*;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.documentation.phpdoc.parser.PhpDocElementTypes;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.dic.ControllerClassOnShortcutReturn;
import fr.adrienbrault.idea.symfony2plugin.routing.dic.ServiceRouteContainer;
import fr.adrienbrault.idea.symfony2plugin.routing.dict.RoutesContainer;
import fr.adrienbrault.idea.symfony2plugin.stubs.SymfonyProcessors;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.StubIndexedRoute;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.AnnotationRoutesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.RoutesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerAction;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.*;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouteHelper {

    private static final Key<CachedValue<Map<String, Route>>> ROUTE_CACHE = new Key<CachedValue<Map<String, Route>>>("SYMFONY:ROUTE_CACHE");

    public static Map<Project, Map<String, RoutesContainer>> COMPILED_CACHE = new HashMap<Project, Map<String, RoutesContainer>>();

    public static LookupElement[] getRouteParameterLookupElements(Project project, String routeName) {
        List<LookupElement> lookupElements = new ArrayList<LookupElement>();

        Route route = RouteHelper.getRoute(project, routeName);
        if(route == null) {
            return lookupElements.toArray(new LookupElement[lookupElements.size()]);
        }

        for(String values: route.getVariables()) {
            lookupElements.add(LookupElementBuilder.create(values).withIcon(Symfony2Icons.ROUTE));
        }

        return lookupElements.toArray(new LookupElement[lookupElements.size()]);
    }

    @Nullable
    public static Route getRoute(Project project, String routeName) {

        Map<String, Route> compiledRoutes = RouteHelper.getCompiledRoutes(project);
        if(!compiledRoutes.containsKey(routeName)) {

            // @TODO: provide multiple ones
            Collection<VirtualFile> foo = FileBasedIndex.getInstance().getContainingFiles(RoutesStubIndex.KEY, routeName, GlobalSearchScope.allScope(project));
            for(String[] str: FileBasedIndex.getInstance().getValues(RoutesStubIndex.KEY, routeName, GlobalSearchScope.filesScope(project, foo))) {
                return new Route(routeName, str);
            }

            return null;
        }

        return compiledRoutes.get(routeName);
    }
    public static PsiElement[] getRouteParameterPsiElements(Project project, String routeName, String parameterName) {

        List<PsiElement> results = new ArrayList<PsiElement>();

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

    public static PsiElement[] getMethods(Project project, String routeName) {

        Route route = getRoute(project, routeName);

        if(route == null) {
            return new PsiElement[0];
        }

        String controllerName = route.getController();
        return getMethodsOnControllerShortcut(project, controllerName);

    }

    /**
     * convert to controller name to method:
     *
     * FooBundle\Controller\BarController::fooBarAction
     * foo_service_bar:fooBar
     * AcmeDemoBundle:Demo:hello
     *
     * @param project current project
     * @param controllerName controller service, raw or compiled
     * @return targets
     */
    @NotNull
    public static PsiElement[] getMethodsOnControllerShortcut(Project project, String controllerName) {

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

                // try to resolve on bundle path
                SymfonyBundle symfonyBundle = new SymfonyBundleUtil(project).getBundle(split[0]);
                if(symfonyBundle != null) {
                    // AcmeDemoBundle\Controller\DemoController:helloAction
                    Method method = PhpElementsUtil.getClassMethod(project, symfonyBundle.getNamespaceName() + "Controller\\" + split[1] + "Controller", split[2] + "Action");
                    if(method != null) {
                        return new PsiElement[] {method};
                    }
                }

                // fallback to controller class instances, if relative path doesnt follow default file structure
                Method method = ControllerIndex.getControllerMethod(project, controllerName);
                if(method != null) {
                    return new PsiElement[] {method};
                }
            }

            // foo_service_bar:fooBar
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
            SymfonyBundle symfonyBundle = new SymfonyBundleUtil(project).getBundle(split[0]);
            if(symfonyBundle != null) {
                PhpClass aClass = PhpElementsUtil.getClass(project, symfonyBundle.getNamespaceName() + "Controller\\" + split[1] + "Controller");
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
        ArrayList<E> list = new ArrayList<E>();
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

    public static Map<String, Route> getCompiledRoutes(Project project) {

        String pathToUrlGenerator = Settings.getInstance(project).pathToUrlGenerator;
        Set<String> files = new HashSet<String>();
        if(pathToUrlGenerator != null) {
            files.add(pathToUrlGenerator);
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
                    COMPILED_CACHE.put(project, new HashMap<String, RoutesContainer>());
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

        Map<String, Route> routes = new HashMap<String, Route>();
        if(COMPILED_CACHE.containsKey(project)) {
            for (RoutesContainer container : COMPILED_CACHE.get(project).values()) {
                routes.putAll(container.getRoutes());
            }
        }

        return routes;
    }

    @NotNull
    public static Map<String, Route> getRoutesInsideUrlGeneratorFile(Project project, VirtualFile virtualFile) {

        Map<String, Route> routes = new HashMap<String, Route>();

        PsiFile psiFile = PsiElementUtils.virtualFileToPsiFile(project, virtualFile);
        if(!(psiFile instanceof PhpFile)) {
            return routes;
        }

        // heavy stuff here, to get nested routing array :)
        // list($variables, $defaults, $requirements, $tokens, $hostTokens)
        Collection<PhpClass> phpClasses = PsiTreeUtil.findChildrenOfType(psiFile, PhpClass.class);
        for(PhpClass phpClass: phpClasses) {
            if(new Symfony2InterfacesUtil().isInstanceOf(phpClass, "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface")) {
                for(Field field: phpClass.getFields()) {
                    if(field.getName().equals("declaredRoutes")) {
                        PsiElement defaultValue = field.getDefaultValue();
                        if(defaultValue instanceof ArrayCreationExpression) {
                            Iterable<ArrayHashElement> arrayHashElements = ((ArrayCreationExpression) defaultValue).getHashElements();
                            for(ArrayHashElement arrayHashElement: arrayHashElements) {

                                PsiElement hashKey = arrayHashElement.getKey();
                                if(hashKey instanceof StringLiteralExpression) {
                                    String routeName = ((StringLiteralExpression) hashKey).getContents();
                                    if(isProductionRouteName(routeName)) {
                                        routeName = convertLanguageRouteName(routeName);
                                        PsiElement hashValue = arrayHashElement.getValue();
                                        if(hashValue instanceof ArrayCreationExpression) {
                                            routes.put(routeName, convertRouteConfig(routeName, (ArrayCreationExpression) hashValue));
                                        }
                                    }

                                }


                            }

                        }

                    }
                }

            }

        }

        return routes;

    }

    private static Route convertRouteConfig(String routeName, ArrayCreationExpression hashValue) {
        List<ArrayHashElement> hashElementCollection = makeCollection(hashValue.getHashElements());

        HashSet<String> variables = new HashSet<String>();
        if(hashElementCollection.size() >= 1 && hashElementCollection.get(0).getValue() instanceof ArrayCreationExpression) {
            variables.addAll(PhpElementsUtil.getArrayKeyValueMap((ArrayCreationExpression) hashElementCollection.get(0).getValue()).values());
        }

        HashMap<String, String> defaults = new HashMap<String, String>();
        if(hashElementCollection.size() >= 2 && hashElementCollection.get(1).getValue() instanceof ArrayCreationExpression) {
            defaults = PhpElementsUtil.getArrayKeyValueMap((ArrayCreationExpression) hashElementCollection.get(1).getValue());
        }

        HashMap<String, String>requirements = new HashMap<String, String>();
        if(hashElementCollection.size() >= 3 && hashElementCollection.get(2).getValue() instanceof ArrayCreationExpression) {
            requirements = PhpElementsUtil.getArrayKeyValueMap((ArrayCreationExpression) hashElementCollection.get(2).getValue());
        }

        ArrayList<Collection<String>> tokens = new ArrayList<Collection<String>>();
        if(hashElementCollection.size() >= 4 && hashElementCollection.get(3).getValue() instanceof ArrayCreationExpression) {
            ArrayCreationExpression tokenArray = (ArrayCreationExpression) hashElementCollection.get(3).getValue();
            if(tokenArray != null) {
                for(ArrayHashElement tokenArrayConfig: tokenArray.getHashElements()) {
                    if(tokenArrayConfig.getValue() instanceof ArrayCreationExpression) {
                        HashMap<String, String> arrayKeyValueMap = PhpElementsUtil.getArrayKeyValueMap((ArrayCreationExpression) tokenArrayConfig.getValue());
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
    public static String convertMethodToRouteControllerName(@NotNull Method method) {

        PhpClass phpClass = method.getContainingClass();
        if(phpClass == null) {
            return null;
        }

        String className = phpClass.getPresentableFQN();
        if(className == null) {
            return null;
        }

        return (className.startsWith("\\") ? className.substring(1) : className) + "::" + method.getName();

    }

    /**
     * FooBundle:Bar::method
     */
    @Nullable
    public static String convertMethodToRouteShortcutControllerName(@NotNull Method method) {

        PhpClass phpClass = method.getContainingClass();
        if(phpClass == null) {
            return null;
        }

        String className = phpClass.getPresentableFQN();
        if(className == null) {
            return null;
        }

        int bundlePos = className.lastIndexOf("Bundle\\");
        if(bundlePos == -1) {
            return null;
        }

        SymfonyBundle symfonyBundle = new SymfonyBundleUtil(method.getProject()).getContainingBundle(phpClass);
        if(symfonyBundle == null) {
            return null;
        }
        String name = method.getName();
        String methodName = name.substring(0, name.length() - "Action".length());

        String relative = symfonyBundle.getRelative(phpClass.getContainingFile().getVirtualFile(), true);
        if(relative == null) {
            return null;
        }

        if(relative.startsWith("Controller/")) {
            relative = relative.substring("Controller/".length());
        }

        if(relative.endsWith("Controller")) {
            relative = relative.substring(0, relative.length() - "Controller".length());
        }

        return String.format("%s:%s:%s", symfonyBundle.getName(), relative.replace("/", "\\"), methodName);
    }

    public static VirtualFile[] getRouteDefinitionInsideFile(Project project, String... routeNames) {

        final List<VirtualFile> virtualFiles = new ArrayList<VirtualFile> ();

        FileBasedIndexImpl.getInstance().getFilesWithKey(RoutesStubIndex.KEY, new HashSet<String>(Arrays.asList(routeNames)), new Processor<VirtualFile>() {
            @Override
            public boolean process(VirtualFile virtualFile) {
                virtualFiles.add(virtualFile);
                return true;
            }
        }, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), YAMLFileType.YML, XmlFileType.INSTANCE));

        FileBasedIndexImpl.getInstance().getFilesWithKey(AnnotationRoutesStubIndex.KEY, new HashSet<String>(Arrays.asList(routeNames)), new Processor<VirtualFile>() {
            @Override
            public boolean process(VirtualFile virtualFile) {
                virtualFiles.add(virtualFile);
                return true;
            }
        }, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), PhpFileType.INSTANCE));

        return virtualFiles.toArray(new VirtualFile[virtualFiles.size()]);

    }

    @NotNull
    public static Collection<StubIndexedRoute> getYamlRouteDefinitions(YAMLDocument yamlDocument) {

        Collection<StubIndexedRoute> indexedRoutes = new ArrayList<StubIndexedRoute>();

        // get services or parameter key
        YAMLKeyValue[] yamlKeys = PsiTreeUtil.getChildrenOfType(yamlDocument, YAMLKeyValue.class);
        if(yamlKeys == null) {
            return Collections.emptyList();
        }

        for(YAMLKeyValue yamlKeyValue : yamlKeys) {

            PsiElement element = yamlKeyValue.getValue();
            if(element instanceof YAMLCompoundValue) {
                Set<String> keySet = YamlHelper.getYamlCompoundValueKeyNames((YAMLCompoundValue) element);

                if((keySet.contains("path") || keySet.contains("pattern"))) {
                    // cleanup: 'foo', "foo"
                    String keyText = StringUtils.strip(StringUtils.strip(yamlKeyValue.getKeyText(), "'"), "\"");
                    if(StringUtils.isNotBlank(keyText)) {
                        StubIndexedRoute route = new StubIndexedRoute(keyText);

                        String routePath = YamlHelper.getYamlKeyValueAsString((YAMLCompoundValue) element, "path", false);
                        if(routePath == null) {
                            routePath = YamlHelper.getYamlKeyValueAsString((YAMLCompoundValue) element, "pattern", false);
                        }

                        if(routePath != null && StringUtils.isNotBlank(routePath)) {
                            route.setPath(routePath);
                        }

                        String controller = getYamlController(yamlKeyValue);
                        if(controller != null) {
                            route.setController(controller);
                        }

                        indexedRoutes.add(route);
                    }
                }
            }
        }

        return indexedRoutes;

    }

    public static Collection<StubIndexedRoute> getXmlRouteDefinitions(XmlFile psiFile) {

        XmlDocumentImpl document = PsiTreeUtil.getChildOfType(psiFile, XmlDocumentImpl.class);
        if(document == null) {
            return Collections.emptyList();
        }

        Collection<StubIndexedRoute> indexedRoutes = new ArrayList<StubIndexedRoute>();

        /**
         * <routes>
         *   <route id="foo" path="/blog/{slug}">
         *     <default key="_controller">Foo</default>
         *   </route>
         * </routes>
         */
        for(XmlTag xmlTag: PsiTreeUtil.getChildrenOfTypeAsList(psiFile.getFirstChild(), XmlTag.class)) {
            if(xmlTag.getName().equals("routes")) {
                for(XmlTag servicesTag: xmlTag.getSubTags()) {
                    if(servicesTag.getName().equals("route")) {
                        XmlAttribute xmlAttribute = servicesTag.getAttribute("id");
                        if(xmlAttribute != null) {
                            String attrValue = xmlAttribute.getValue();
                            if(StringUtils.isNotBlank(attrValue)) {

                                StubIndexedRoute e = new StubIndexedRoute(attrValue);
                                XmlAttribute pathAttribute = servicesTag.getAttribute("path");
                                if(pathAttribute == null) {
                                    pathAttribute = servicesTag.getAttribute("pattern");
                                }

                                if(pathAttribute != null) {
                                    String pathValue = pathAttribute.getValue();
                                    if(pathValue != null && StringUtils.isNotBlank(pathValue)) {
                                        e.setPath(pathValue);
                                    }
                                }

                                for(XmlTag subTag :servicesTag.getSubTags()) {
                                    if("default".equalsIgnoreCase(subTag.getName())) {
                                        XmlAttribute xmlAttr = subTag.getAttribute("key");
                                        if(xmlAttr != null && "_controller".equals(xmlAttr.getValue())) {
                                            String actionName = subTag.getValue().getTrimmedText();
                                            if(StringUtils.isNotBlank(actionName)) {
                                                e.setController(actionName);
                                            }
                                        }
                                    }
                                }

                                indexedRoutes.add(e);

                            }
                        }
                    }
                }
            }
        }

        return indexedRoutes;
    }

    @Nullable
    private static String getYamlController(YAMLKeyValue psiElement) {
        /*
         * foo:
         *   defaults: { _controller: "Bundle:Foo:Bar" }
         *   defaults:
         *      _controller: "Bundle:Foo:Bar"
         */

        YAMLKeyValue yamlKeyValue = YamlHelper.getYamlKeyValue(psiElement, "defaults");
        if(yamlKeyValue != null) {
            YAMLCompoundValue yamlCompoundValue = PsiTreeUtil.getChildOfType(yamlKeyValue, YAMLCompoundValue.class);
            if(yamlCompoundValue != null) {

                // if we have a child of YAMLKeyValue, we need to go back to parent
                // else on YAMLHash we can directly visit array keys
                PsiElement yamlHashElement = PsiTreeUtil.getChildOfAnyType(yamlCompoundValue, YAMLHash.class, YAMLKeyValue.class);
                if(yamlHashElement instanceof YAMLKeyValue) {
                    yamlHashElement = yamlCompoundValue;
                }

                if(yamlHashElement != null) {
                    YAMLKeyValue yamlKeyValueController = YamlHelper.getYamlKeyValue(yamlHashElement, "_controller", true);
                    if(yamlKeyValueController != null) {
                        String valueText = yamlKeyValueController.getValueText();
                        if(StringUtils.isNotBlank(valueText)) {
                            return valueText;
                        }
                    }

                }
            }

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
        return !shortcutName.contains("::") && shortcutName.contains(":") && !shortcutName.contains("\\") && shortcutName.split(":").length == 2;
    }

    @Nullable
    public static List<Route> getRoutesOnControllerAction(@NotNull Method method) {

        Set<String> routeNames = new HashSet<String>();

        String methodRouteActionName = RouteHelper.convertMethodToRouteControllerName(method);
        if(methodRouteActionName != null) {
            routeNames.add(methodRouteActionName);
        }

        String shortcutName = RouteHelper.convertMethodToRouteShortcutControllerName(method);
        if(shortcutName != null) {
            routeNames.add(shortcutName);
        }

        Map<String, Route> allRoutes = getAllRoutes(method.getProject());
        List<Route> routes = new ArrayList<Route>();

        // resolve indexed routes
        if(routeNames.size() > 0) {
            for(Map.Entry<String, Route> routeEntry: allRoutes.entrySet()) {
                String controller = routeEntry.getValue().getController();
                if(controller != null && routeNames.contains(controller)) {
                    routes.add(routeEntry.getValue());
                }
            }
        }

        // search for services
        Collection<Route> methodMatch = ServiceRouteContainer.build(allRoutes).getMethodMatches(method);
        if(methodMatch.size() > 0) {
            routes.addAll(methodMatch);
        }

        return routes;
    }

    @Nullable
    public static PsiElement getRouteNameTarget(Project project, String routeName) {

        VirtualFile[] virtualFiles = RouteHelper.getRouteDefinitionInsideFile(project, routeName);
        for(VirtualFile virtualFile: virtualFiles) {

            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);

            if(psiFile instanceof YAMLFile) {
                YAMLKeyValue yamlKeyValue = YamlHelper.getRootKey(psiFile, routeName);
                if(yamlKeyValue != null) {
                    return yamlKeyValue;
                }
            }

            if(psiFile instanceof XmlFile) {
                PsiElement target = RouteHelper.getXmlRouteNameTarget((XmlFile) psiFile, routeName);
                if(target != null) {
                    return target;
                }
            }

            if(psiFile instanceof PhpFile) {

                Collection<PhpDocTag> phpDocTagList = PsiTreeUtil.findChildrenOfType(psiFile, PhpDocTag.class);
                for(PhpDocTag phpDocTag: phpDocTagList) {

                    String annotationFqnName = AnnotationRoutesStubIndex.getClassNameReference(phpDocTag);
                    if("\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Route".equals(annotationFqnName)) {
                        PsiElement phpDocAttributeList = PsiElementUtils.getChildrenOfType(phpDocTag, PlatformPatterns.psiElement(PhpDocElementTypes.phpDocAttributeList));
                        if(phpDocAttributeList != null) {
                            // @TODO: use pattern
                            Matcher matcher = Pattern.compile("name\\s*=\\s*\"(\\w+)\"").matcher(phpDocAttributeList.getText());
                            if (matcher.find() && matcher.group(1).equals(routeName)) {
                                return phpDocAttributeList;
                            }
                        }
                    }

                }

            }

        }

        return null;
    }

    @Nullable
    public static String getRouteUrl(Route route) {

        if(route.getPath() != null) {
            return route.getPath();
        }

        String url = "";

        // copy list;
        List<Collection<String>> tokens = new ArrayList<Collection<String>>(route.getTokens());
        Collections.reverse(tokens);

        for(Collection<String> token: tokens) {

            // copy, we are not allowed to mod list
            List<String> list = new ArrayList<String>(token);

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

        final List<LookupElement> lookupElements = new ArrayList<LookupElement>();

        final Set<String> uniqueSet = new HashSet<String>();
        for (Route route : routes.values()) {
            lookupElements.add(new RouteLookupElement(route));
            uniqueSet.add(route.getName());
        }

        SymfonyProcessors.CollectProjectUniqueKeysStrong ymlProjectProcessor = new SymfonyProcessors.CollectProjectUniqueKeysStrong(project, RoutesStubIndex.KEY, uniqueSet);
        FileBasedIndex.getInstance().processAllKeys(RoutesStubIndex.KEY, ymlProjectProcessor, project);
        for(String routeName: ymlProjectProcessor.getResult()) {
            for(String[] splits: FileBasedIndex.getInstance().getValues(RoutesStubIndex.KEY, routeName, GlobalSearchScope.allScope(project))) {
                lookupElements.add(new RouteLookupElement(new Route(routeName, splits), true));
                uniqueSet.add(routeName);
            }
        }

        SymfonyProcessors.CollectProjectUniqueKeysStrong annotationProjectProcessor = new SymfonyProcessors.CollectProjectUniqueKeysStrong(project, AnnotationRoutesStubIndex.KEY, uniqueSet);
        FileBasedIndex.getInstance().processAllKeys(AnnotationRoutesStubIndex.KEY, annotationProjectProcessor, project);
        for(String s: annotationProjectProcessor.getResult()) {
            lookupElements.add(new RouteLookupElement(new Route(s), true));
            uniqueSet.add(s);
        }

        return lookupElements;

    }

    public static List<PsiElement> getRouteDefinitionTargets(Project project, String routeName) {

        List<PsiElement> targets = new ArrayList<PsiElement>();
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
            cache = CachedValuesManager.getManager(project).createCachedValue(new CachedValueProvider<Map<String, Route>>() {
                @Nullable
                @Override
                public Result<Map<String, Route>> compute() {
                    return Result.create(getAllRoutesProxy(project), PsiModificationTracker.MODIFICATION_COUNT);
                }
            }, false);
            project.putUserData(ROUTE_CACHE, cache);
        }

        return cache.getValue();
    }

    @NotNull
    private static Map<String, Route> getAllRoutesProxy(@NotNull Project project) {

        Map<String, Route> routes = new HashMap<String, Route>();
        routes.putAll(RouteHelper.getCompiledRoutes(project));

        Set<String> uniqueKeySet = new HashSet<String>(routes.keySet());

        SymfonyProcessors.CollectProjectUniqueKeysStrong ymlProjectProcessor = new SymfonyProcessors.CollectProjectUniqueKeysStrong(project, RoutesStubIndex.KEY, uniqueKeySet);
        FileBasedIndex.getInstance().processAllKeys(RoutesStubIndex.KEY, ymlProjectProcessor, project);
        for(String routeName: ymlProjectProcessor.getResult()) {

            if(uniqueKeySet.contains(routeName)) {
                continue;
            }

            for(String[] splits: FileBasedIndex.getInstance().getValues(RoutesStubIndex.KEY, routeName, GlobalSearchScope.allScope(project))) {
                uniqueKeySet.add(routeName);
                routes.put(routeName, new Route(routeName, splits));
            }
        }

        SymfonyProcessors.CollectProjectUniqueKeysStrong annotationProjectProcessor = new SymfonyProcessors.CollectProjectUniqueKeysStrong(project, AnnotationRoutesStubIndex.KEY, uniqueKeySet);
        FileBasedIndex.getInstance().processAllKeys(AnnotationRoutesStubIndex.KEY, annotationProjectProcessor, project);
        for(String routeName: annotationProjectProcessor.getResult()) {

            if(uniqueKeySet.contains(routeName)) {
                continue;
            }

            routes.put(routeName, new Route(routeName));
        }

        return routes;
    }

}
