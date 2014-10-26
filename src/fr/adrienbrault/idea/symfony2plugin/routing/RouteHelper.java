package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.xml.XmlDocumentImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
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
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.SymfonyProcessors;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.StubIndexedRoute;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.AnnotationRoutesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.YamlRoutesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerAction;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.*;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouteHelper {

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

        Symfony2ProjectComponent symfony2ProjectComponent = project.getComponent(Symfony2ProjectComponent.class);

        if(!symfony2ProjectComponent.getRoutes().containsKey(routeName)) {

            // @TODO: provide multiple ones
            Collection<VirtualFile> foo = FileBasedIndex.getInstance().getContainingFiles(YamlRoutesStubIndex.KEY, routeName, GlobalSearchScope.allScope(project));
            for(String[] str: FileBasedIndex.getInstance().getValues(YamlRoutesStubIndex.KEY, routeName, GlobalSearchScope.filesScope(project, foo))) {
                return new Route(routeName, str);
            }

            return null;
        }

        return symfony2ProjectComponent.getRoutes().get(routeName);
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

    private static <E> ArrayList<E> makeCollection(Iterable<E> iter) {
        ArrayList<E> list = new ArrayList<E>();
        for (E item : iter) {
            list.add(item);
        }
        return list;
    }

    public static Map<String, Route> getRoutes(Project project, VirtualFile virtualFile) {

        Map<String, Route> routes = new HashMap<String, Route>();

        try {
            routes.putAll(getRoutes(VfsUtil.loadText(virtualFile)));
        } catch (IOException ignored) {
        }

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

    public static Map<String, Route> getRoutes(String routing) {
        Map<String, Route> routes = new HashMap<String, Route>();

        Matcher matcher = Pattern.compile("'((?:[^'\\\\]|\\\\.)*)' => [^\\n]+'_controller' => '((?:[^'\\\\]|\\\\.)*)'[^\\n]+\n").matcher(routing);

        while (matcher.find()) {
            String routeName = matcher.group(1);

            // dont add _assetic_04d92f8, _assetic_04d92f8_0
            if(!isProductionRouteName(routeName)) {
               continue;
            }

            routeName = convertLanguageRouteName(routeName);

            String controller = matcher.group(2).replace("\\\\", "\\");
            Route route = new Route(routeName, controller);
            routes.put(route.getName(), route);

        }

        return routes;
    }


    /**
     * Foo\Bar::methodAction
     */
    @Nullable
    public static String convertMethodToRouteControllerName(Method method) {

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

    public static VirtualFile[] getRouteDefinitionInsideFile(Project project, String... routeNames) {

        final List<VirtualFile> virtualFiles = new ArrayList<VirtualFile> ();

        FileBasedIndexImpl.getInstance().getFilesWithKey(YamlRoutesStubIndex.KEY, new HashSet<String>(Arrays.asList(routeNames)), new Processor<VirtualFile>() {
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

                if((keySet.contains("path") || keySet.contains("pattern")) && keySet.contains("defaults")) {
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

    @Nullable
    public static List<Route> getRoutesOnControllerAction(Method method) {

        String methodRouteActionName = RouteHelper.convertMethodToRouteControllerName(method);
        if(methodRouteActionName == null) {
            return null;
        }

        List<Route> routes = new ArrayList<Route>();

        Symfony2ProjectComponent symfony2ProjectComponent = method.getProject().getComponent(Symfony2ProjectComponent.class);
        for(Map.Entry<String, Route> routeEntry: symfony2ProjectComponent.getRoutes().entrySet()) {
            if(routeEntry.getValue().getController() != null && routeEntry.getValue().getController().equals(methodRouteActionName)) {
                routes.add(routeEntry.getValue());
            }
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

        Symfony2ProjectComponent symfony2ProjectComponent = project.getComponent(Symfony2ProjectComponent.class);
        Map<String, Route> routes = symfony2ProjectComponent.getRoutes();

        final List<LookupElement> lookupElements = new ArrayList<LookupElement>();

        final Set<String> uniqueSet = new HashSet<String>();
        for (Route route : routes.values()) {
            lookupElements.add(new RouteLookupElement(route));
            uniqueSet.add(route.getName());
        }

        SymfonyProcessors.CollectProjectUniqueKeysStrong ymlProjectProcessor = new SymfonyProcessors.CollectProjectUniqueKeysStrong(project, YamlRoutesStubIndex.KEY, uniqueSet);
        FileBasedIndex.getInstance().processAllKeys(YamlRoutesStubIndex.KEY, ymlProjectProcessor, project);
        for(String s: ymlProjectProcessor.getResult()) {
            lookupElements.add(new RouteLookupElement(new Route(s), true));
            uniqueSet.add(s);
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

    public static Map<String, Route> getAllRoutes(Project project) {

        Map<String, Route> routes = new HashMap<String, Route>();

        Symfony2ProjectComponent symfony2ProjectComponent = project.getComponent(Symfony2ProjectComponent.class);
        routes.putAll(symfony2ProjectComponent.getRoutes());

        SymfonyProcessors.CollectProjectUniqueKeysStrong ymlProjectProcessor = new SymfonyProcessors.CollectProjectUniqueKeysStrong(project, YamlRoutesStubIndex.KEY, routes.keySet());
        FileBasedIndex.getInstance().processAllKeys(YamlRoutesStubIndex.KEY, ymlProjectProcessor, project);
        for(String routeName: ymlProjectProcessor.getResult()) {
            for(String[] splits: FileBasedIndex.getInstance().getValues(YamlRoutesStubIndex.KEY, routeName, GlobalSearchScope.allScope(project))) {
                routes.put(routeName, new Route(routeName, splits));
            }
        }

        return routes;
    }

}
