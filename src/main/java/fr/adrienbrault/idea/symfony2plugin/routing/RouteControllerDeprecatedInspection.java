package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.patterns.ElementPattern;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpClassMember;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteControllerDeprecatedInspection {
    public static class RouteControllerDeprecatedXmlLocalInspectionTool extends LocalInspectionTool {
        @Override
        public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
            Project project = holder.getProject();
            if (!Symfony2ProjectComponent.isEnabled(project)) {
                return super.buildVisitor(holder, isOnTheFly);
            }

            return new MyXmlPsiElementVisitor(project, holder);
        }

        private static class MyXmlPsiElementVisitor extends PsiElementVisitor {
            @NotNull private final Project project;
            @NotNull private final ProblemsHolder holder;
            private ElementPattern<?> routeControllerPattern;

            MyXmlPsiElementVisitor(@NotNull Project project, @NotNull ProblemsHolder holder) {
                this.project = project;
                this.holder = holder;
            }

            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (getRouteControllerPattern().accepts(element)) {
                    PsiElement parent = element.getParent();
                    if (parent != null) {
                        String text = RouteXmlReferenceContributor.getControllerText(parent);
                        if(text != null) {
                            hasDeprecatedActionOrClass(project, element, text, holder);
                        }
                    }
                }

                super.visitElement(element);
            }

            private ElementPattern<?> getRouteControllerPattern() {
                return routeControllerPattern != null ? routeControllerPattern : (routeControllerPattern = XmlHelper.getRouteControllerPattern());
            }
        }
    }

    public static class RouteControllerDeprecatedYamlLocalInspectionTool extends LocalInspectionTool {
        @Override
        public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
            Project project = holder.getProject();
            if (!Symfony2ProjectComponent.isEnabled(project)) {
                return super.buildVisitor(holder, isOnTheFly);
            }

            return new MyYamlPsiElementVisitor(project, holder);
        }

        private static class MyYamlPsiElementVisitor extends PsiElementVisitor {
            @NotNull private final Project project;
            @NotNull private final ProblemsHolder holder;
            private ElementPattern<?> controllerScalarPattern;

            MyYamlPsiElementVisitor(@NotNull Project project, @NotNull ProblemsHolder holder) {
                this.project = project;
                this.holder = holder;
            }

            @Override
            public void visitElement(@NotNull PsiElement element) {
                if(getControllerScalarPattern().accepts(element)) {
                    String text = PsiElementUtils.trimQuote(element.getText());
                    if (StringUtils.isNotBlank(text)) {
                        hasDeprecatedActionOrClass(project, element, text, holder);
                    }
                }

                super.visitElement(element);
            }

            private ElementPattern<?> getControllerScalarPattern() {
                return controllerScalarPattern != null ? controllerScalarPattern : (controllerScalarPattern = YamlElementPatternHelper.getSingleLineScalarKey("_controller", "controller"));
            }
        }
    }

    private static void hasDeprecatedActionOrClass(@NotNull Project project, @NotNull PsiElement element, String text, @NotNull ProblemsHolder holder) {
        for (PsiElement psiElement : RouteHelper.getMethodsOnControllerShortcut(project, text)) {
            if (!(psiElement instanceof PhpNamedElement phpNamedElement)) {
                continue;
            }

            if (psiElement instanceof Method method && PhpElementsUtil.isClassOrFunctionDeprecated(method)) {
                holder.registerProblem(element, "Symfony: Controller action is deprecated", ProblemHighlightType.LIKE_DEPRECATED);
                break;
            }

            if (psiElement instanceof PhpClassMember phpClassMember) {
                PhpClass containingClass = phpClassMember.getContainingClass();
                if (containingClass != null && PhpElementsUtil.isClassOrFunctionDeprecated(containingClass)) {
                    holder.registerProblem(element, "Symfony: Controller action is deprecated", ProblemHighlightType.LIKE_DEPRECATED);
                    break;
                }
            }
        }
    }
}