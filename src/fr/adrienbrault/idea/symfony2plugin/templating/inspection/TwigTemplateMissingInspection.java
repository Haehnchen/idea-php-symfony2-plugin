package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * {% include 'f<caret>.html.twig' %}
 * {{ include('f<caret>.html.twig') }}
 * {% embed 'f<caret>.html.twig' %}
 * ... and so on
 *
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTemplateMissingInspection extends LocalInspectionTool {
    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if((TwigPattern.getTemplateFileReferenceTagPattern().accepts(element) || TwigPattern.getPrintBlockFunctionPattern("include", "source").accepts(element)) && TwigUtil.isValidStringWithoutInterpolatedOrConcat(element)) {
                    invoke(element, holder);
                }

                super.visitElement(element);
            }
        };
    }

    private void invoke(@NotNull final PsiElement element, @NotNull ProblemsHolder holder) {
        String templateName = element.getText();

        Collection<VirtualFile> psiElements = TwigUtil.getTemplateFiles(element.getProject(), templateName);
        if(psiElements.size() > 0)  {
            return;
        }

        holder.registerProblem(
            element,
            "Twig: Missing Template",
            new TemplateCreateByNameLocalQuickFix(templateName)
        );
    }
}
