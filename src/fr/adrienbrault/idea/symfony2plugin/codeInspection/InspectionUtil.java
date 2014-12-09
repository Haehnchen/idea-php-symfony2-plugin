package fr.adrienbrault.idea.symfony2plugin.codeInspection;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.codeInspection.quickfix.CreateMethodQuickFix;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class InspectionUtil {

    public static void inspectController(@NotNull PsiElement psiElement, @NotNull String controllerName, @NotNull ProblemsHolder holder) {

        int lastPos = controllerName.lastIndexOf(":") + 1;
        final String actionName = controllerName.substring(lastPos) + "Action";

        List<PsiElement> psiElements = Arrays.asList(RouteHelper.getMethodsOnControllerShortcut(psiElement.getProject(), controllerName));
        if(psiElements.size() > 0) {
            return;
        }

        PhpClass phpClass = RouteHelper.getControllerClassOnShortcut(psiElement.getProject(), controllerName);
        if(phpClass == null) {
            return;
        }

        holder.registerProblem(psiElement, "Create Method", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new CreateMethodQuickFix(phpClass, actionName, new CreateMethodQuickFix.InsertStringInterface() {
            @NotNull
            @Override
            public StringBuilder getStringBuilder() {
                return new StringBuilder()
                    .append("public function ")
                    .append(actionName)
                    .append("()\n {\n}\n\n");
            }
        }));

    }

}
