package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.patterns.ElementPattern;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.apache.commons.lang3.StringUtils;
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

        return new MyPsiElementVisitor(holder);
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        @NotNull
        private final ProblemsHolder holder;

        private ElementPattern<?> templateFileReferencePattern;
        private ElementPattern<?> includeFunctionPattern;

        MyPsiElementVisitor(@NotNull ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            if((getTemplateFileReferencePattern().accepts(element) || getIncludeFunctionPattern().accepts(element)) && TwigUtil.isValidStringWithoutInterpolatedOrConcat(element)) {
                invoke(element, holder);
            }

            super.visitElement(element);
        }

        private ElementPattern<?> getTemplateFileReferencePattern() {
            return templateFileReferencePattern != null ? templateFileReferencePattern : (templateFileReferencePattern = TwigPattern.getTemplateFileReferenceTagPattern());
        }

        private ElementPattern<?> getIncludeFunctionPattern() {
            return includeFunctionPattern != null ? includeFunctionPattern : (includeFunctionPattern = TwigPattern.getPrintBlockOrTagFunctionPattern("include", "source"));
        }
    }

    private static void invoke(@NotNull final PsiElement element, @NotNull ProblemsHolder holder) {
        String templateName = element.getText();
        if (StringUtils.isBlank(templateName)) {
            return;
        }

        Collection<VirtualFile> psiElements = TwigUtil.getTemplateFiles(element.getProject(), templateName);
        if(!psiElements.isEmpty())  {
            return;
        }

        LocalQuickFix[] templateCreateByNameLocalQuickFix = new LocalQuickFix[]{
            new TemplateCreateByNameLocalQuickFix(templateName),
            new TemplateGuessTypoQuickFix(templateName)
        };

        holder.registerProblem(
            element,
            "Twig: Missing Template",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            templateCreateByNameLocalQuickFix
        );
    }
}
