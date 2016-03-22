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
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
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
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitFile(PsiFile psiFile) {
                if(psiFile instanceof YAMLFile) {
                    psiFile.acceptChildren(new YmlClassElementWalkingVisitor(holder, new ContainerCollectionResolver.LazyServiceCollector(holder.getProject())));
                } else if(psiFile instanceof XmlFile) {
                    psiFile.acceptChildren(new XmlClassElementWalkingVisitor(holder, new ContainerCollectionResolver.LazyServiceCollector(holder.getProject())));
                }
            }
        };
    }

    private class XmlClassElementWalkingVisitor extends PsiRecursiveElementWalkingVisitor {
        private final ProblemsHolder holder;
        private final ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector;

        public XmlClassElementWalkingVisitor(ProblemsHolder holder, ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {
            this.holder = holder;
            this.lazyServiceCollector = lazyServiceCollector;
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
                        registerTaggedProblems(psiElements[1], FormUtil.getTags(parentOfType), text, holder, this.lazyServiceCollector);
                    }
                }
            }

            super.visitElement(element);
        }
    }

    private class YmlClassElementWalkingVisitor extends PsiRecursiveElementWalkingVisitor {
        private final ProblemsHolder holder;
        private final ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector;

        public YmlClassElementWalkingVisitor(ProblemsHolder holder, ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {
            this.holder = holder;
            this.lazyServiceCollector = lazyServiceCollector;
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
                            Set<String> tags = YamlHelper.collectServiceTags((YAMLKeyValue) serviceKeyValue);
                            if(tags != null && tags.size() > 0) {
                                registerTaggedProblems(element, tags, text, holder, this.lazyServiceCollector);
                            }
                        }

                    }
                }

            }


            super.visitElement(element);
        }

    }

    private void registerTaggedProblems(@NotNull PsiElement source, @NotNull Set<String> tags, @NotNull String serviceClass, @NotNull ProblemsHolder holder, @NotNull ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {

        if(tags.size() == 0) {
            return;
        }

        PhpClass phpClass = null;

        for (String tag : tags) {

            if(!ServiceUtil.TAG_INTERFACES.containsKey(tag)) {
                continue;
            }

            String expectedClass = ServiceUtil.TAG_INTERFACES.get(tag);
            if(expectedClass == null) {
                continue;
            }

            // load PhpClass only if we need it, on error exit
            if(phpClass == null) {
                phpClass = ServiceUtil.getResolvedClassDefinition(holder.getProject(), serviceClass, lazyServiceCollector);
                if(phpClass == null) {
                    return;
                }
            }

            // check interfaces
            if(!PhpElementsUtil.isInstanceOf(phpClass, expectedClass)) {
                holder.registerProblem(source, String.format("Class needs to implement '%s' for tag '%s'", expectedClass, tag), ProblemHighlightType.WEAK_WARNING);
            }

        }

    }

}
