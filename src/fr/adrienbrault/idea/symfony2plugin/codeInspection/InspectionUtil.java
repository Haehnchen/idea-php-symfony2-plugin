package fr.adrienbrault.idea.symfony2plugin.codeInspection;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.codeInspection.quickfix.CreateMethodQuickFix;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.routing.dic.ControllerClassOnShortcutReturn;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

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
        holder.registerProblem(psiElement, "Create Method", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new CreateMethodQuickFix(shortcutReturn.getPhpClass(), actionName, new CreateMethodQuickFix.InsertStringInterface() {
            @NotNull
            @Override
            public StringBuilder getStringBuilder() {

                // attach route parameter inside method
                String parameters = "";
                String routeName = lazyControllerNameResolve.getRouteName();
                if(routeName != null) {
                    Route route = RouteHelper.getRoute(project, routeName);
                    if(route != null) {
                        Set<String> vars = route.getVariables();
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
            }
        }));

    }

    public interface LazyControllerNameResolve {
        @Nullable
        public String getRouteName();
    }

}
