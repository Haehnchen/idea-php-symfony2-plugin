package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.YamlRoutesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerAction;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLKeyValue;

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

    public static PsiElement[] getMethodsOnControllerShortcut(Project project, String controllerName) {

        if(controllerName == null)  {
            return new PsiElement[0];
        }

        // convert to class: FooBundle\Controller\BarController::fooBarAction
        // convert to class: foo_service_bar:fooBar
        if(controllerName.contains("::")) {
            String className = controllerName.substring(0, controllerName.lastIndexOf("::"));
            String methodName = controllerName.substring(controllerName.lastIndexOf("::") + 2);

            return PhpElementsUtil.getPsiElementsBySignature(project, "#M#C\\" + className + "." + methodName);

        } else if(controllerName.contains(":")) {
            ControllerIndex controllerIndex = new ControllerIndex(project);

            ControllerAction controllerServiceAction = controllerIndex.getControllerActionOnService(controllerName);
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
        }, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), YAMLFileType.YML));

        return virtualFiles.toArray(new VirtualFile[virtualFiles.size()]);

    }

    @Nullable
    public static Set<String> getYamlRouteNames(YAMLDocument yamlDocument) {

        Set<String> set = new HashSet<String>();

        // get services or parameter key
        YAMLKeyValue[] yamlKeys = PsiTreeUtil.getChildrenOfType(yamlDocument, YAMLKeyValue.class);
        if(yamlKeys == null) {
            return null;
        }

        for(YAMLKeyValue yamlKeyValue : yamlKeys) {

            PsiElement element = yamlKeyValue.getValue();
            if(element instanceof YAMLCompoundValue) {
                Set<String> keySet = YamlHelper.getYamlCompoundValueKeyNames((YAMLCompoundValue) element);
                if((keySet.contains("path") || keySet.contains("pattern")) && keySet.contains("defaults")) {
                    set.add(yamlKeyValue.getKeyText());
                }
            }
        }

        return set;

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
            if(psiFile != null) {

                YAMLKeyValue yamlKeyValue = YamlHelper.getRootKey(psiFile, routeName);
                if(yamlKeyValue != null) {
                    return yamlKeyValue;
                }
            }
        }

        return null;
    }

    @Nullable
    public static String getRouteUrl(List<Collection<String>> routeTokens) {

        String url = "";

        // copy list;
        List<Collection<String>> tokens = new ArrayList<Collection<String>>(routeTokens);
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

    public static List<LookupElement> getRoutesLookupElements(Project project) {

        Symfony2ProjectComponent symfony2ProjectComponent = project.getComponent(Symfony2ProjectComponent.class);
        Map<String, Route> routes = symfony2ProjectComponent.getRoutes();

        final List<LookupElement> lookupElements = new ArrayList<LookupElement>();

        final Set<String> uniqueSet = new HashSet<String>();
        for (Route route : routes.values()) {
            lookupElements.add(new RouteLookupElement(route));
            uniqueSet.add(route.getName());
        }

        FileBasedIndexImpl.getInstance().processAllKeys(YamlRoutesStubIndex.KEY, new Processor<String>() {
            @Override
            public boolean process(String s) {

                if(!uniqueSet.contains(s)) {
                    lookupElements.add(new RouteLookupElement(new Route(s, null), true));
                }

                return true;
            }
        }, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), YAMLFileType.YML), null);

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

}
