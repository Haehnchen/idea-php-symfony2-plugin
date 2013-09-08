package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerAction;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouteHelper {

    public static PsiElement[] getMethods(Project project, String routeName) {

        Symfony2ProjectComponent symfony2ProjectComponent = project.getComponent(Symfony2ProjectComponent.class);
        Map<String,Route> routes = symfony2ProjectComponent.getRoutes();

        for (Route route : routes.values()) {
            if(route.getName().equals(routeName)) {
                String controllerName = route.getController();

                // convert to class: FooBundle\Controller\BarController::fooBarAction
                // convert to class: foo_service_bar:fooBar
                if(controllerName.contains("::")) {
                    String className = controllerName.substring(0, controllerName.lastIndexOf("::"));
                    String methodName = controllerName.substring(controllerName.lastIndexOf("::") + 2);

                    PhpIndex phpIndex = PhpIndex.getInstance(project);
                    Collection<? extends PhpNamedElement> methodCalls = phpIndex.getBySignature("#M#C\\" + className + "." + methodName, null, 0);
                    return methodCalls.toArray(new PsiElement[methodCalls.size()]);

                } else if(controllerName.contains(":")) {
                    ControllerIndex controllerIndex = new ControllerIndex(project);

                    ControllerAction controllerServiceAction = controllerIndex.getControllerActionOnService(controllerName);
                    if(controllerServiceAction != null) {
                        return new PsiElement[] {controllerServiceAction.getMethod()};
                    }

                }

                return new PsiElement[0];
            }

        }

        return new PsiElement[0];
    }

    public static Map<String, Route> getRoutes(String routing) {
        Map<String, Route> routes = new HashMap<String, Route>();

        Matcher matcher = Pattern.compile("'((?:[^'\\\\]|\\\\.)*)' => [^\\n]+'_controller' => '((?:[^'\\\\]|\\\\.)*)'[^\\n]+\n").matcher(routing);

        if (null == matcher) {
            return routes;
        }

        while (matcher.find()) {
            String routeName = matcher.group(1);

            // dont add _assetic_04d92f8, _assetic_04d92f8_0
            if(routeName.matches("_assetic_[0-9a-z]+[_\\d+]*")) {
               continue;
            }

            // support I18nRoutingBundle
            if(routeName.matches("^[a-z]{2}__RG__.*$")) {
                routeName = routeName.replaceAll("^[a-z]{2}+__RG__", "");
            }

            String controller = matcher.group(2).replace("\\\\", "\\");
            Route route = new Route(routeName, controller);
            routes.put(route.getName(), route);

        }

        return routes;
    }

}
