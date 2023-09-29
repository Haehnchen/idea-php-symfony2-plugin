package fr.adrienbrault.idea.symfony2plugin.codeInspection;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.codeInspection.quickfix.CreateMethodQuickFix;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.routing.dic.ControllerClassOnShortcutReturn;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class InspectionUtil {

    public static void inspectController(@NotNull PsiElement psiElement, @NotNull String controllerName, @NotNull ProblemsHolder holder, final @NotNull LazyControllerNameResolve lazyControllerNameResolve) {

        List<PsiElement> psiElements = Arrays.asList(RouteHelper.getMethodsOnControllerShortcut(psiElement.getProject(), controllerName));
        if(psiElements.size() > 0) {
            return;
        }

        ControllerClassOnShortcutReturn shortcutReturn = RouteHelper.getControllerClassOnShortcut(psiElement.getProject(), controllerName);
        if(shortcutReturn == null) {
            return;
        }

        int lastPos = controllerName.lastIndexOf(":") + 1;
        String actionName = controllerName.substring(lastPos);
        if(!shortcutReturn.isService()) {
            actionName += "Action";
        }

        final Project project = shortcutReturn.getPhpClass().getProject();
        final String finalActionName = actionName;
        holder.registerProblem(psiElement, "Create Method", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new CreateMethodQuickFix(shortcutReturn.getPhpClass(), actionName, (@NotNull ProblemDescriptor problemDescriptor, @NotNull PhpClass phpClass, @NotNull String functionName) -> {

            // attach route parameter inside method
            String parameters = "";
            String routeName = lazyControllerNameResolve.getRouteName();
            if(routeName != null) {
                Collection<Route> routes = RouteHelper.getRoute(project, routeName);
                if(routes.size() > 0) {
                    Set<String> vars = routes.iterator().next().getVariables();
                    if(vars.size() > 0) {

                        // add dollar char for vars
                        List<String> varsDollar = new ArrayList<>();
                        for(String var: vars) {
                            varsDollar.add("$" + var);
                        }

                        parameters = StringUtils.join(varsDollar, ", ");
                    }
                }
            }

            return new StringBuilder()
                .append("public function ")
                .append(finalActionName)
                .append("(")
                .append(parameters)
                .append(")\n {\n}\n\n");
        }));

    }

    public interface LazyControllerNameResolve {
        @Nullable
        String getRouteName();
    }

}
