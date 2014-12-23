package fr.adrienbrault.idea.symfony2plugin.routing.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.xml.XmlFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInspection.InspectionUtil;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLFile;

public class ControllerMethodInspection extends LocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {

        PsiFile psiFile = holder.getFile();
        if(!Symfony2ProjectComponent.isEnabled(psiFile.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }


        if(psiFile instanceof YAMLFile) {
            visitYaml(holder, psiFile);
        }

        if(psiFile instanceof XmlFile) {
            visitXml(holder, psiFile);
        }

        return super.buildVisitor(holder, isOnTheFly);
    }

    private void visitYaml(final ProblemsHolder holder, PsiFile psiFile) {
        psiFile.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if(YamlElementPatternHelper.getSingleLineScalarKey("_controller").accepts(element)) {
                    String text = PsiElementUtils.trimQuote(element.getText());
                    if(StringUtils.isNotBlank(text)) {
                        InspectionUtil.inspectController(element, text, holder, null);
                    }
                }

                super.visitElement(element);
            }
        });
    }

    private void visitXml(final ProblemsHolder holder, PsiFile psiFile) {
        psiFile.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if(XmlHelper.getRouteConfigControllerPattern().accepts(element)) {
                    String text = PsiElementUtils.trimQuote(element.getText());
                    if(StringUtils.isNotBlank(text)) {
                        InspectionUtil.inspectController(element, text, holder, null);
                    }
                }

                super.visitElement(element);
            }
        });
    }

}
