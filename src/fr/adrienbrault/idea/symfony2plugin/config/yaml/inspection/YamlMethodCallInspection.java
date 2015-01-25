package fr.adrienbrault.idea.symfony2plugin.config.yaml.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
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
import org.jetbrains.annotations.Nullable;
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

    @Nullable
    private String getEventName(PsiElement psiElement) {

        // xml service
        if(psiElement.getContainingFile() instanceof XmlFile) {

            XmlTag xmlTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
            if(xmlTag == null) {
                return null;
            }

            XmlAttribute event = xmlTag.getAttribute("event");
            if(event == null) {
                return null;
            }

            String value = event.getValue();
            if(StringUtils.isBlank(value)) {
                return null;
            }

            return value;

        } else if(psiElement.getContainingFile() instanceof YAMLFile) {

            // yaml services
            YAMLHash yamlHash = PsiTreeUtil.getParentOfType(psiElement, YAMLHash.class);
            if(yamlHash != null) {
                YAMLKeyValue event = YamlHelper.getYamlKeyValue(yamlHash, "event");
                if(event != null) {
                    PsiElement value = event.getValue();
                    if(value != null ) {
                        String text = value.getText();
                        if(StringUtils.isNotBlank(text)) {
                            return text;
                        }
                    }
                }
            }

        }

        return null;
    }

    private void visitYamlMethodTagKey(@NotNull final PsiElement psiElement, @NotNull ProblemsHolder holder) {

        String methodName = PsiElementUtils.trimQuote(psiElement.getText());
        if(StringUtils.isBlank(methodName)) {
            return;
        }

        String classValue = YamlHelper.getServiceDefinitionClass(psiElement);
        if(classValue == null) {
            return;
        }

        registerMethodProblem(psiElement, holder, classValue);
    }

    private void annotateCallMethod(@NotNull final PsiElement psiElement, @NotNull ProblemsHolder holder) {

        if(StandardPatterns.and(
            YamlElementPatternHelper.getInsideKeyValue("tags"),
            YamlElementPatternHelper.getSingleLineScalarKey("method")
        ).accepts(psiElement)) {
            visitYamlMethodTagKey(psiElement, holder);
        }

        if((PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).accepts(psiElement)
            || PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_DSTRING).accepts(psiElement)))
        {
            visitYamlMethod(psiElement, holder);
        }

    }

    private void visitYamlMethod(PsiElement psiElement, ProblemsHolder holder) {
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

    @Nullable
    private static String getTaggedEventMethodParameter(Project project, String eventName) {

        if(ServiceUtil.TAGS.containsKey(eventName)) {
            return ServiceUtil.TAGS.get(eventName);
        }

        /*
        @TODO: add live service event tags
        ContainerCollectionResolver.ServiceCollector containerCollectionResolver = new ContainerCollectionResolver.ServiceCollector(project);
        for (String service : ServiceUtil.getTaggedServices(project, "kernel.event_listener")) {
            for (VirtualFile virtualFile : FileBasedIndexImpl.getInstance().getContainingFiles(ServicesTagStubIndex.KEY, service, GlobalSearchScope.allScope(project))) {

                PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                if(psiFile != null) {

                }
            }
        }
        */

        return null;
    }

    private void registerMethodProblem(final @NotNull PsiElement psiElement, @NotNull ProblemsHolder holder, @NotNull String classKeyValue) {

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

                String taggedEventMethodParameter = null;
                String eventName = getEventName(psiElement);
                if(eventName != null) {
                    taggedEventMethodParameter = getTaggedEventMethodParameter(psiElement.getProject(), eventName);
                }

                String parameter = "";
                if(taggedEventMethodParameter != null) {
                    parameter = taggedEventMethodParameter + " $event";
                }

                return new StringBuilder()
                    .append("public function ")
                    .append(methodName)
                    .append("(")
                    .append(parameter)
                    .append(")\n {\n}\n\n");
            }
        }));
    }

    private String getServiceName(PsiElement psiElement) {
        return YamlHelper.trimSpecialSyntaxServiceName(PsiElementUtils.getText(psiElement));
    }

}
