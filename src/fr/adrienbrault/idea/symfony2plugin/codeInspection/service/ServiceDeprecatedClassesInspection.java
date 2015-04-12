package fr.adrienbrault.idea.symfony2plugin.codeInspection.service;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLFile;


public class ServiceDeprecatedClassesInspection extends LocalInspectionTool {

    private ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector;

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

        if(psiFile instanceof PhpFile) {
            psiFile.acceptChildren(new PhpClassWalkingVisitor(holder));
        }

        this.lazyServiceCollector = null;

        return super.buildVisitor(holder, isOnTheFly);
    }

    private void attachDeprecatedProblem(PsiElement element, String text, ProblemsHolder holder) {

        PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(element.getProject(), text, getLazyServiceCollector(element.getProject()));
        if(phpClass == null) {
            return;
        }

        PhpDocComment docComment = phpClass.getDocComment();
        if(docComment != null && docComment.getTagElementsByName("@deprecated").length > 0) {
            holder.registerProblem(element, String.format("Class '%s' is deprecated", phpClass.getName()), ProblemHighlightType.LIKE_DEPRECATED);
        }

    }


    private class XmlClassElementWalkingVisitor extends PsiRecursiveElementWalkingVisitor {
        private final ProblemsHolder holder;

        public XmlClassElementWalkingVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            if(XmlHelper.getServiceIdPattern().accepts(element) || XmlHelper.getArgumentServiceIdPattern().accepts(element)) {
                String text = PsiElementUtils.trimQuote(element.getText());
                PsiElement[] psiElements = element.getChildren();

                // we need to attach to child because else strike out equal and quote char
                if(StringUtils.isNotBlank(text) && psiElements.length > 2) {
                    attachDeprecatedProblem(psiElements[1], text, holder);
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
                if(StringUtils.isNotBlank(text)) {
                    attachDeprecatedProblem(element, text, holder);
                }
            } else if(element.getNode().getElementType() == YAMLTokenTypes.TEXT) {
                // @service
                String text = element.getText();
                if(text != null && StringUtils.isNotBlank(text) && text.startsWith("@")) {
                    attachDeprecatedProblem(element, text.substring(1), holder);
                }
            }

            super.visitElement(element);
        }
    }

    private class PhpClassWalkingVisitor extends PsiRecursiveElementWalkingVisitor {

        private final ProblemsHolder holder;
        Symfony2InterfacesUtil symfony2InterfacesUtil;

        public PhpClassWalkingVisitor(ProblemsHolder holder) {
            this.holder = holder;
            symfony2InterfacesUtil = new Symfony2InterfacesUtil();
        }

        @Override
        public void visitElement(PsiElement element) {

            MethodReference methodReference = PsiElementUtils.getMethodReferenceWithFirstStringParameter(element);
            if (methodReference == null || !symfony2InterfacesUtil.isContainerGetCall(methodReference)) {
                super.visitElement(element);
                return;
            }

            PsiElement psiElement = element.getParent();
            if(!(psiElement instanceof StringLiteralExpression)) {
                super.visitElement(element);
                return;
            }

            String contents = ((StringLiteralExpression) psiElement).getContents();
            if(StringUtils.isNotBlank(contents)) {
                attachDeprecatedProblem(element, contents, holder);
            }

            super.visitElement(element);
        }
    }

    private ContainerCollectionResolver.LazyServiceCollector getLazyServiceCollector(Project project) {
        return this.lazyServiceCollector == null ? this.lazyServiceCollector = new ContainerCollectionResolver.LazyServiceCollector(project) : this.lazyServiceCollector;
    }

}
