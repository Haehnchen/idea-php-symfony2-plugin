package fr.adrienbrault.idea.symfony2plugin.codeInspection.service;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.Set;


public class TaggedExtendsInterfaceClassInspection extends LocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {

        final PsiFile psiFile = holder.getFile();
        if(!Symfony2ProjectComponent.isEnabled(psiFile.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        if(psiFile instanceof YAMLFile) {
            psiFile.acceptChildren(new YmlClassElementWalkingVisitor(holder));
        }

        if(psiFile instanceof XmlFile) {
            psiFile.acceptChildren(new XmlClassElementWalkingVisitor(holder));
        }

        return super.buildVisitor(holder, isOnTheFly);
    }

    private class XmlClassElementWalkingVisitor extends PsiRecursiveElementWalkingVisitor {
        private final ProblemsHolder holder;

        public XmlClassElementWalkingVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            if(XmlHelper.getServiceIdPattern().accepts(element)) {
                String text = PsiElementUtils.trimQuote(element.getText());
                PsiElement[] psiElements = element.getChildren();

                // attach problems to string value only
                if(StringUtils.isNotBlank(text) && psiElements.length > 2) {
                    XmlTag parentOfType = PsiTreeUtil.getParentOfType(element, XmlTag.class);
                    if(parentOfType != null) {
                        registerTaggedProblems(psiElements[1], FormUtil.getTags(parentOfType), text, holder);
                    }
                }
            }

            super.visitElement(element);
        }
    }

    private class YmlClassElementWalkingVisitor extends PsiRecursiveElementWalkingVisitor {
        private final ProblemsHolder holder;

        public YmlClassElementWalkingVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {

            if(YamlElementPatternHelper.getSingleLineScalarKey("class").accepts(element)) {

                // class: '\Foo'
                String text = PsiElementUtils.trimQuote(element.getText());
                if(StringUtils.isBlank(text)) {
                    super.visitElement(element);
                    return;
                }

                PsiElement classKey = element.getParent();
                if(classKey instanceof YAMLKeyValue) {
                    PsiElement yamlCompoundValue = classKey.getParent();
                    if(yamlCompoundValue instanceof YAMLCompoundValue) {
                        PsiElement serviceKeyValue = yamlCompoundValue.getParent();
                        if(serviceKeyValue instanceof YAMLKeyValue) {
                            registerTaggedProblems(element, FormUtil.getTags((YAMLKeyValue) serviceKeyValue), text, holder);
                        }

                    }
                }

            }


            super.visitElement(element);
        }

    }

    private void registerTaggedProblems(@NotNull PsiElement source, @NotNull Set<String> tags, @NotNull String serviceClass, @NotNull ProblemsHolder holder) {

        if(tags.size() == 0) {
            return;
        }

        PhpClass phpClass = null;

        for (String tag : tags) {

            if(!ServiceUtil.TAG_INTERFACES.containsKey(tag)) {
                continue;
            }

            // load PhpClass only if we need it, on error exit
            if(phpClass == null) {
                phpClass = ServiceUtil.getResolvedClassDefinition(source.getProject(), serviceClass);
                if(phpClass == null) {
                    return;
                }
            }

            // check interfaces
            String expectedClass = ServiceUtil.TAG_INTERFACES.get(tag);
            if(!new Symfony2InterfacesUtil().isInstanceOf(phpClass, expectedClass)) {
                holder.registerProblem(source, String.format("Class needs to implement '%s' for tag '%s'", expectedClass, tag), ProblemHighlightType.WEAK_WARNING);
            }

        }

    }

}
