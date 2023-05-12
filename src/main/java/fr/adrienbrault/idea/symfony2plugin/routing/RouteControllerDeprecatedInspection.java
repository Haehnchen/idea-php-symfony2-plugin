package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
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
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteControllerDeprecatedInspection extends LocalInspectionTool {

    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        Project project = holder.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (XmlHelper.getRouteControllerPattern().accepts(element)) {
                    PsiElement parent = element.getParent();
                    if (parent != null) {
                        String text = RouteXmlReferenceContributor.getControllerText(parent);
                        if(text != null) {
                            extracted(project, element, text, holder);
                        }
                    }
                } else if(YamlElementPatternHelper.getSingleLineScalarKey("_controller", "controller").accepts(element)) {
                    String text = PsiElementUtils.trimQuote(element.getText());
                    if (StringUtils.isNotBlank(text)) {
                        extracted(project, element, text, holder);
                    }
                }

                super.visitElement(element);
            }
        };
    }

    private void extracted(@NotNull Project project, @NotNull PsiElement element, String text, @NotNull ProblemsHolder holder) {
        for (PsiElement psiElement : RouteHelper.getMethodsOnControllerShortcut(project, text)) {
            if (!(psiElement instanceof PhpNamedElement)) {
                continue;
            }

            // action is deprecated
            if (((PhpNamedElement) psiElement).isDeprecated()) {
                holder.registerProblem(element, "Symfony: Controller action is deprecated", ProblemHighlightType.LIKE_DEPRECATED);
                break;
            }

            // class is deprecated
            if (psiElement instanceof PhpClassMember) {
                PhpClass containingClass = ((Method) psiElement).getContainingClass();
                if (containingClass != null && containingClass.isDeprecated()) {
                    holder.registerProblem(element, "Symfony: Controller action is deprecated", ProblemHighlightType.LIKE_DEPRECATED);
                    break;
                }
            }
        }
    }
}