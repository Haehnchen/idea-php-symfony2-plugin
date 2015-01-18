package fr.adrienbrault.idea.symfony2plugin.config.yaml.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInspection.quickfix.CreateMethodQuickFix;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.*;

public class YamlMethodCallInspection extends LocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {

        PsiFile psiFile = holder.getFile();
        if(!Symfony2ProjectComponent.isEnabled(psiFile.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        if(psiFile instanceof XmlFile) {
            visitXmlFile(psiFile, holder);
        } else if(psiFile instanceof YAMLFile) {
            visitYamlFile(psiFile, holder);
        }

        return super.buildVisitor(holder, isOnTheFly);
    }

    private void visitYamlFile(PsiFile psiFile, final ProblemsHolder holder) {

        psiFile.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                annotateCallMethod(element, holder);
                super.visitElement(element);
            }
        });


        YAMLDocument document = PsiTreeUtil.findChildOfType(psiFile, YAMLDocument.class);
        if(document != null) {
            YAMLKeyValue yamlKeyValue = YamlHelper.getYamlKeyValue(document, "services");
            if(yamlKeyValue != null) {
                YAMLCompoundValue yaml = PsiTreeUtil.findChildOfType(yamlKeyValue, YAMLCompoundValue.class);
                if(yaml != null) {
                    YamlHelper.attachDuplicateKeyInspection(yaml, holder);
                }

            }
        }
    }

    private void visitXmlFile(@NotNull PsiFile psiFile, @NotNull final ProblemsHolder holder) {

        psiFile.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {

                if(XmlHelper.getTagAttributePattern("tag", "method").inside(XmlHelper.getInsideTagPattern("services")).inFile(XmlHelper.getXmlFilePattern()).accepts(element) ||
                   XmlHelper.getTagAttributePattern("call", "method").inside(XmlHelper.getInsideTagPattern("services")).inFile(XmlHelper.getXmlFilePattern()).accepts(element)
                  )
                {

                    // attach to text child only
                    PsiElement[] psiElements = element.getChildren();
                    if(psiElements.length < 2) {
                        return;
                    }

                    String serviceClassValue = XmlHelper.getServiceDefinitionClass(element);
                    if(serviceClassValue != null && StringUtils.isNotBlank(serviceClassValue)) {
                        registerMethodProblem(psiElements[1], holder, serviceClassValue);
                    }

                }

                super.visitElement(element);
            }
        });

    }

    private void annotateCallMethod(@NotNull final PsiElement psiElement, @NotNull ProblemsHolder holder) {

        if((!PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).accepts(psiElement)
            && !PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_DSTRING).accepts(psiElement)))
        {
            return;
        }

        if(!YamlElementPatternHelper.getInsideKeyValue("calls").accepts(psiElement)){
            return;
        }

        if(psiElement.getParent() == null || !(psiElement.getParent().getContext() instanceof YAMLSequence)) {
            return;
        }

        YAMLKeyValue callYamlKeyValue = PsiTreeUtil.getParentOfType(psiElement, YAMLKeyValue.class);
        if(callYamlKeyValue == null) {
            return;
        }

        YAMLKeyValue classKeyValue = YamlHelper.getYamlKeyValue(callYamlKeyValue.getContext(), "class");
        if(classKeyValue == null) {
            return;
        }

        registerMethodProblem(psiElement, holder, getServiceName(classKeyValue.getValue()));

    }

    private void registerMethodProblem(@NotNull PsiElement psiElement, @NotNull ProblemsHolder holder, @NotNull String classKeyValue) {

        PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), classKeyValue);
        if(phpClass == null) {
            return;
        }

        final String methodName = PsiElementUtils.trimQuote(psiElement.getText());
        if(PhpElementsUtil.getClassMethod(phpClass, methodName) != null) {
            return;
        }

        holder.registerProblem(psiElement, "Create Method", ProblemHighlightType.WEAK_WARNING, new CreateMethodQuickFix(phpClass, methodName, new CreateMethodQuickFix.InsertStringInterface() {
            @NotNull
            @Override
            public StringBuilder getStringBuilder() {

                return new StringBuilder()
                    .append("public function ")
                    .append(methodName)
                    .append("(")
                    // .append(parameters) @TODO: use class or service name
                    .append(")\n {\n}\n\n");
            }
        }));
    }

    private String getServiceName(PsiElement psiElement) {
        return YamlHelper.trimSpecialSyntaxServiceName(PsiElementUtils.getText(psiElement));
    }

}
