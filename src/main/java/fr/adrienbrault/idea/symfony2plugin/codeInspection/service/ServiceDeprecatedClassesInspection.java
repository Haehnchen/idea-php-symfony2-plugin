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
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceInterface;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceDeprecatedClassesInspection extends LocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }
        return new MyPsiElementVisitor(holder);
    }

    private static class ProblemRegistrar {

        private ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector;

        public void attachDeprecatedProblem(PsiElement element, String text, ProblemsHolder holder) {

            PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(element.getProject(), text, getLazyServiceCollector(element.getProject()));
            if(phpClass == null) {
                return;
            }

            PhpDocComment docComment = phpClass.getDocComment();
            if(docComment != null && docComment.getTagElementsByName("@deprecated").length > 0) {
                holder.registerProblem(element, String.format("Class '%s' is deprecated", phpClass.getName()), ProblemHighlightType.LIKE_DEPRECATED);
            }

        }

        public void attachServiceDeprecatedProblem(@NotNull PsiElement element, @NotNull String serviceName, @NotNull ProblemsHolder holder) {

            Map<String, ContainerService> services = getLazyServiceCollector(element.getProject()).getCollector().getServices();
            if(!services.containsKey(serviceName)) {
                return;
            }

            ServiceInterface serviceDef = services.get(serviceName).getService();
            if(serviceDef == null || !serviceDef.isDeprecated()) {
                return;
            }

            holder.registerProblem(element, String.format("Service '%s' is deprecated", serviceName), ProblemHighlightType.LIKE_DEPRECATED);
        }

        private ContainerCollectionResolver.LazyServiceCollector getLazyServiceCollector(Project project) {
            return this.lazyServiceCollector == null ? this.lazyServiceCollector = new ContainerCollectionResolver.LazyServiceCollector(project) : this.lazyServiceCollector;
        }

        public void reset() {
            this.lazyServiceCollector = null;
        }
    }


    private class XmlClassElementWalkingVisitor extends PsiRecursiveElementWalkingVisitor {
        private final ProblemsHolder holder;
        private final ProblemRegistrar problemRegistrar;

        public XmlClassElementWalkingVisitor(ProblemsHolder holder, ProblemRegistrar problemRegistrar) {
            this.holder = holder;
            this.problemRegistrar = problemRegistrar;
        }

        @Override
        public void visitElement(@NotNull PsiElement element) {
            boolean serviceArgumentAccepted = XmlHelper.getArgumentServiceIdPattern().accepts(element);
            if(serviceArgumentAccepted || XmlHelper.getServiceClassAttributeWithIdPattern().accepts(element)) {
                String text = PsiElementUtils.trimQuote(element.getText());
                PsiElement[] psiElements = element.getChildren();

                // we need to attach to child because else strike out equal and quote char
                if(StringUtils.isNotBlank(text) && psiElements.length > 2) {
                    this.problemRegistrar.attachDeprecatedProblem(psiElements[1], text, holder);

                    // check service arguments for "deprecated" defs
                    if(serviceArgumentAccepted) {
                        this.problemRegistrar.attachServiceDeprecatedProblem(psiElements[1], text, holder);
                    }
                }
            }

            super.visitElement(element);
        }
    }

    private class YmlClassElementWalkingVisitor extends PsiRecursiveElementWalkingVisitor {
        private final ProblemsHolder holder;
        private final ProblemRegistrar problemRegistrar;

        public YmlClassElementWalkingVisitor(ProblemsHolder holder, ProblemRegistrar problemRegistrar) {
            this.holder = holder;
            this.problemRegistrar = problemRegistrar;
        }

        @Override
        public void visitElement(PsiElement element) {

            if(YamlElementPatternHelper.getSingleLineScalarKey("class").accepts(element)) {
                // class: '\Foo'
                String text = PsiElementUtils.trimQuote(element.getText());
                if(StringUtils.isNotBlank(text)) {
                    this.problemRegistrar.attachDeprecatedProblem(element, text, holder);
                }
            } else if(element.getNode().getElementType() == YAMLTokenTypes.TEXT) {
                // @service
                String text = element.getText();
                if(StringUtils.isNotBlank(text) && text.startsWith("@")) {
                    this.problemRegistrar.attachDeprecatedProblem(element, text.substring(1), holder);
                    this.problemRegistrar.attachServiceDeprecatedProblem(element, text.substring(1), holder);
                }
            }

            super.visitElement(element);
        }
    }

    private class PhpClassWalkingVisitor extends PsiRecursiveElementWalkingVisitor {
        @NotNull
        private final ProblemsHolder holder;

        @NotNull
        private final ProblemRegistrar problemRegistrar;

        private PhpClassWalkingVisitor(@NotNull ProblemsHolder holder, @NotNull ProblemRegistrar problemRegistrar) {
            this.holder = holder;
            this.problemRegistrar = problemRegistrar;
        }

        @Override
        public void visitElement(PsiElement psiElement) {
            if (!(psiElement instanceof StringLiteralExpression)) {
                super.visitElement(psiElement);
                return;
            }

            // #[Autowire(service: 'foobar')]
            PsiElement leafText = PsiElementUtils.getTextLeafElementFromStringLiteralExpression((StringLiteralExpression) psiElement);

            if (leafText != null && PhpElementsUtil.getAttributeNamedArgumentStringPattern(ServiceContainerUtil.AUTOWIRE_ATTRIBUTE_CLASS, "service").accepts(leafText)) {
                String contents = ((StringLiteralExpression) psiElement).getContents();
                if(StringUtils.isNotBlank(contents)) {
                    this.problemRegistrar.attachDeprecatedProblem(psiElement, contents, holder);
                    this.problemRegistrar.attachServiceDeprecatedProblem(psiElement, contents, holder);
                }

                super.visitElement(psiElement);
                return;
            }

            MethodReference methodReference = PsiElementUtils.getMethodReferenceWithFirstStringParameter((StringLiteralExpression) psiElement);
            if (methodReference == null || !PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, ServiceContainerUtil.SERVICE_GET_SIGNATURES)) {
                super.visitElement(psiElement);
                return;
            }

            String contents = ((StringLiteralExpression) psiElement).getContents();
            if(StringUtils.isNotBlank(contents)) {
                this.problemRegistrar.attachDeprecatedProblem(psiElement, contents, holder);
                this.problemRegistrar.attachServiceDeprecatedProblem(psiElement, contents, holder);
            }

            super.visitElement(psiElement);
        }
    }

    private class MyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder holder;

        public MyPsiElementVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitFile(PsiFile psiFile) {

            ProblemRegistrar problemRegistrar = null;

            if(psiFile instanceof YAMLFile) {
                psiFile.acceptChildren(new YmlClassElementWalkingVisitor(holder, problemRegistrar = new ProblemRegistrar()));
            } else if(psiFile instanceof XmlFile) {
                psiFile.acceptChildren(new XmlClassElementWalkingVisitor(holder, problemRegistrar = new ProblemRegistrar()));
            } else if(psiFile instanceof PhpFile) {
                psiFile.acceptChildren(new PhpClassWalkingVisitor(holder, problemRegistrar = new ProblemRegistrar()));
            }

            if(problemRegistrar != null) {
                problemRegistrar.reset();
            }

            super.visitFile(psiFile);
        }
    }
}
