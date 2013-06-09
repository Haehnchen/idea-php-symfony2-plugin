package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;

import java.util.Collection;
import java.util.Map;

public class RouteHelper {

    public static PsiElement[] getMethods(Project project, String routeName) {

        Symfony2ProjectComponent symfony2ProjectComponent = project.getComponent(Symfony2ProjectComponent.class);
        Map<String,Route> routes = symfony2ProjectComponent.getRoutes();

        for (Route route : routes.values()) {
            if(route.getName().equals(routeName)) {
                String controllerName = route.getController();

                // convert to class: FooBundle\Controller\BarController::fooBarAction
                if(controllerName.contains("::")) {
                    String className = controllerName.substring(0, controllerName.lastIndexOf("::"));
                    String methodName = controllerName.substring(controllerName.lastIndexOf("::") +2);

                    PhpIndex phpIndex = PhpIndex.getInstance(project);
                    Collection<? extends PhpNamedElement> methodCalls = phpIndex.getBySignature("#M#C\\" + className + "." + methodName, null, 0);
                    return methodCalls.toArray(new PsiElement[methodCalls.size()]);
                }

                return new PsiElement[0];
            }

        }

        return new PsiElement[0];
    }
}
