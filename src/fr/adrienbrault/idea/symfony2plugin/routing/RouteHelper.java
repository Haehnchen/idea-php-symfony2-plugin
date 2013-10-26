package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerAction;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouteHelper {

    @Nullable
    public static Route getRoute(Project project, String routeName) {

        Symfony2ProjectComponent symfony2ProjectComponent = project.getComponent(Symfony2ProjectComponent.class);

        if(!symfony2ProjectComponent.getRoutes().containsKey(routeName)) {
            return null;
        }

        return symfony2ProjectComponent.getRoutes().get(routeName);
    }

    public static PsiElement[] getMethods(Project project, String routeName) {

        Route route = getRoute(project, routeName);

        if(route == null) {
            return new PsiElement[0];
        }

        String controllerName = route.getController();
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

}
