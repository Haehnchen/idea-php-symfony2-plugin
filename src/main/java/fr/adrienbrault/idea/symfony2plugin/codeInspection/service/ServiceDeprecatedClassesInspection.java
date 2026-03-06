package fr.adrienbrault.idea.symfony2plugin.codeInspection.service;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.patterns.ElementPattern;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
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
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLTokenTypes;

import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceDeprecatedClassesInspection {
    public static class ServiceDeprecatedClassesInspectionYaml extends LocalInspectionTool {
        public @NotNull PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
            if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
                return super.buildVisitor(holder, isOnTheFly);
            }

            return new MyYamlPsiElementVisitor(holder);
        }

        private static class MyYamlPsiElementVisitor extends PsiElementVisitor {
            @NotNull private final ProblemsHolder holder;
            private NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector> serviceCollector;
            private ElementPattern<?> singleLineClassPattern;

            MyYamlPsiElementVisitor(@NotNull ProblemsHolder holder) {
                this.holder = holder;
            }

            @Override
            public void visitElement(@NotNull PsiElement element) {
                visitYamlElement(element, holder);
                super.visitElement(element);
            }

            private void visitYamlElement(@NotNull PsiElement element, @NotNull ProblemsHolder holder) {
                if (getSingleLineClassPattern().accepts(element)) {
                    // class: '\Foo'
                    String text = PsiElementUtils.trimQuote(element.getText());
                    if (StringUtils.isNotBlank(text)) {
                        ProblemRegistrar.attachDeprecatedProblem(element, text, holder, createLazyServiceCollector());
                    }
                } else if (element.getNode().getElementType() == YAMLTokenTypes.TEXT) {
                    // @service
                    String text = element.getText();
                    if (StringUtils.isNotBlank(text) && text.startsWith("@")) {
                        ProblemRegistrar.attachDeprecatedProblem(element, text.substring(1), holder, createLazyServiceCollector());
                        ProblemRegistrar.attachServiceDeprecatedProblem(element, text.substring(1), holder, createLazyServiceCollector());
                    }
                }
            }

            private NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector> createLazyServiceCollector() {
                if (this.serviceCollector == null) {
                    this.serviceCollector = NotNullLazyValue.lazy(() -> new ContainerCollectionResolver.LazyServiceCollector(holder.getProject()));
                }

                return this.serviceCollector;
            }

            private ElementPattern<?> getSingleLineClassPattern() {
                return singleLineClassPattern != null ? singleLineClassPattern : (singleLineClassPattern = YamlElementPatternHelper.getSingleLineScalarKey("class"));
            }
        }
    }

    public static class ServiceDeprecatedClassesInspectionXml extends LocalInspectionTool {
        public @NotNull PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
            if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
                return super.buildVisitor(holder, isOnTheFly);
            }

            return new MyXmlPsiElementVisitor(holder);
        }

        private static class MyXmlPsiElementVisitor extends PsiElementVisitor {
            @NotNull private final ProblemsHolder holder;
            private NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector> serviceCollector;
            private ElementPattern<?> argumentServiceIdPattern;
            private ElementPattern<?> serviceClassAttributePattern;

            MyXmlPsiElementVisitor(@NotNull ProblemsHolder holder) {
                this.holder = holder;
            }

            @Override
            public void visitElement(@NotNull PsiElement element) {
                visitXmlElement(element, holder);
                super.visitElement(element);
            }

            private void visitXmlElement(@NotNull PsiElement element, @NotNull ProblemsHolder holder) {
                boolean serviceArgumentAccepted = getArgumentServiceIdPattern().accepts(element);

                if (serviceArgumentAccepted || getServiceClassAttributePattern().accepts(element)) {
                    String text = PsiElementUtils.trimQuote(element.getText());
                    PsiElement[] psiElements = element.getChildren();

                    // we need to attach to child because else strike out equal and quote char
                    if (StringUtils.isNotBlank(text) && psiElements.length > 2) {
                        ProblemRegistrar.attachDeprecatedProblem(psiElements[1], text, holder, createLazyServiceCollector());

                        // check service arguments for "deprecated" defs
                        if (serviceArgumentAccepted) {
                            ProblemRegistrar.attachServiceDeprecatedProblem(psiElements[1], text, holder, createLazyServiceCollector());
                        }
                    }
                }
            }

            private NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector> createLazyServiceCollector() {
                if (this.serviceCollector == null) {
                    this.serviceCollector = NotNullLazyValue.lazy(() -> new ContainerCollectionResolver.LazyServiceCollector(holder.getProject()));
                }

                return this.serviceCollector;
            }

            private ElementPattern<?> getArgumentServiceIdPattern() {
                return argumentServiceIdPattern != null ? argumentServiceIdPattern : (argumentServiceIdPattern = XmlHelper.getArgumentServiceIdPattern());
            }

            private ElementPattern<?> getServiceClassAttributePattern() {
                return serviceClassAttributePattern != null ? serviceClassAttributePattern : (serviceClassAttributePattern = XmlHelper.getServiceClassAttributeWithIdPattern());
            }
        }
    }

    public static class ServiceDeprecatedClassesInspectionPhp extends LocalInspectionTool {
        public @NotNull PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
            if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
                return super.buildVisitor(holder, isOnTheFly);
            }

            return new MyPhpPsiElementVisitor(holder);
        }

        private static class MyPhpPsiElementVisitor extends PsiElementVisitor {
            @NotNull private final ProblemsHolder holder;
            private NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector> serviceCollector;
            private ElementPattern<?> autowireServicePattern;

            MyPhpPsiElementVisitor(@NotNull ProblemsHolder holder) {
                this.holder = holder;
            }

            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof StringLiteralExpression stringLiteralExpression) {
                    visitPhpElement(stringLiteralExpression, holder);
                }

                super.visitElement(element);
            }

            private void visitPhpElement(@NotNull StringLiteralExpression psiElement, @NotNull ProblemsHolder holder) {
                // #[Autowire(service: 'foobar')]
                PsiElement leafText = PsiElementUtils.getTextLeafElementFromStringLiteralExpression(psiElement);

                if (leafText != null && getAutowireServicePattern().accepts(leafText)) {
                    String contents = psiElement.getContents();
                    if (StringUtils.isNotBlank(contents)) {
                        ProblemRegistrar.attachDeprecatedProblem(psiElement, contents, holder, createLazyServiceCollector());
                        ProblemRegistrar.attachServiceDeprecatedProblem(psiElement, contents, holder, createLazyServiceCollector());
                    }

                    return;
                }

                MethodReference methodReference = PsiElementUtils.getMethodReferenceWithFirstStringParameter(psiElement);
                if (methodReference == null || !PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, ServiceContainerUtil.SERVICE_GET_SIGNATURES)) {
                    return;
                }

                String contents = psiElement.getContents();
                if (StringUtils.isNotBlank(contents)) {
                    ProblemRegistrar.attachDeprecatedProblem(psiElement, contents, holder, createLazyServiceCollector());
                    ProblemRegistrar.attachServiceDeprecatedProblem(psiElement, contents, holder, createLazyServiceCollector());
                }
            }

            private NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector> createLazyServiceCollector() {
                if (this.serviceCollector == null) {
                    this.serviceCollector = NotNullLazyValue.lazy(() -> new ContainerCollectionResolver.LazyServiceCollector(holder.getProject()));
                }

                return this.serviceCollector;
            }

            private ElementPattern<?> getAutowireServicePattern() {
                return autowireServicePattern != null ? autowireServicePattern : (autowireServicePattern = PhpElementsUtil.getAttributeNamedArgumentStringPattern(ServiceContainerUtil.AUTOWIRE_ATTRIBUTE_CLASS, "service"));
            }
        }
    }

    private static class ProblemRegistrar {
        public static void attachDeprecatedProblem(@NotNull PsiElement element, @NotNull String text, @NotNull ProblemsHolder holder, @NotNull NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector> lazyServiceCollector) {
            PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(element.getProject(), text, lazyServiceCollector.get());
            if(phpClass == null) {
                return;
            }

            PhpDocComment docComment = phpClass.getDocComment();
            if(docComment != null && docComment.getTagElementsByName("@deprecated").length > 0) {
                holder.registerProblem(element, String.format("Class '%s' is deprecated", phpClass.getName()), ProblemHighlightType.LIKE_DEPRECATED);
            }
        }

        public static void attachServiceDeprecatedProblem(@NotNull PsiElement element, @NotNull String serviceName, @NotNull ProblemsHolder holder, @NotNull NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector> lazyServiceCollector) {
            Map<String, ContainerService> services = lazyServiceCollector.get().getCollector().getServices();
            if(!services.containsKey(serviceName)) {
                return;
            }

            ServiceInterface serviceDef = services.get(serviceName).getService();
            if(serviceDef == null || !serviceDef.isDeprecated()) {
                return;
            }

            holder.registerProblem(element, String.format("Service '%s' is deprecated", serviceName), ProblemHighlightType.LIKE_DEPRECATED);
        }
    }
}
