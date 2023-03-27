package fr.adrienbrault.idea.symfony2plugin.codeInspection.service;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.Language;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.lang.PhpLanguage;
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
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.YAMLTokenTypes;

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

    private void visitXmlElement(@NotNull PsiElement element, @NotNull ProblemsHolder holder, @NotNull NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector> lazyServiceCollector) {
        boolean serviceArgumentAccepted = XmlHelper.getArgumentServiceIdPattern().accepts(element);

        if(serviceArgumentAccepted || XmlHelper.getServiceClassAttributeWithIdPattern().accepts(element)) {
            String text = PsiElementUtils.trimQuote(element.getText());
            PsiElement[] psiElements = element.getChildren();

            // we need to attach to child because else strike out equal and quote char
            if(StringUtils.isNotBlank(text) && psiElements.length > 2) {
                ProblemRegistrar.attachDeprecatedProblem(psiElements[1], text, holder, lazyServiceCollector);

                // check service arguments for "deprecated" defs
                if(serviceArgumentAccepted) {
                    ProblemRegistrar.attachServiceDeprecatedProblem(psiElements[1], text, holder, lazyServiceCollector);
                }
            }
        }
    }

    private void visitYamlElement(@NotNull PsiElement element, @NotNull ProblemsHolder holder, NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector> lazyServiceCollector) {

        if(YamlElementPatternHelper.getSingleLineScalarKey("class").accepts(element)) {
            // class: '\Foo'
            String text = PsiElementUtils.trimQuote(element.getText());
            if(StringUtils.isNotBlank(text)) {
                ProblemRegistrar.attachDeprecatedProblem(element, text, holder, lazyServiceCollector);
            }
        } else if(element.getNode().getElementType() == YAMLTokenTypes.TEXT) {
            // @service
            String text = element.getText();
            if(StringUtils.isNotBlank(text) && text.startsWith("@")) {
                ProblemRegistrar.attachDeprecatedProblem(element, text.substring(1), holder, lazyServiceCollector);
                ProblemRegistrar.attachServiceDeprecatedProblem(element, text.substring(1), holder, lazyServiceCollector);
            }
        }
    }

    private void visitPhpElement(@NotNull StringLiteralExpression psiElement, @NotNull ProblemsHolder holder, NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector> lazyServiceCollector) {
        // #[Autowire(service: 'foobar')]
        PsiElement leafText = PsiElementUtils.getTextLeafElementFromStringLiteralExpression(psiElement);

        if (leafText != null && PhpElementsUtil.getAttributeNamedArgumentStringPattern(ServiceContainerUtil.AUTOWIRE_ATTRIBUTE_CLASS, "service").accepts(leafText)) {
            String contents = psiElement.getContents();
            if(StringUtils.isNotBlank(contents)) {
                ProblemRegistrar.attachDeprecatedProblem(psiElement, contents, holder, lazyServiceCollector);
                ProblemRegistrar.attachServiceDeprecatedProblem(psiElement, contents, holder, lazyServiceCollector);
            }

            return;
        }

        MethodReference methodReference = PsiElementUtils.getMethodReferenceWithFirstStringParameter(psiElement);
        if (methodReference == null || !PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, ServiceContainerUtil.SERVICE_GET_SIGNATURES)) {
            return;
        }

        String contents = psiElement.getContents();
        if(StringUtils.isNotBlank(contents)) {
            ProblemRegistrar.attachDeprecatedProblem(psiElement, contents, holder, lazyServiceCollector);
            ProblemRegistrar.attachServiceDeprecatedProblem(psiElement, contents, holder, lazyServiceCollector);
        }
    }

    private class MyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder holder;
        private NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector> serviceCollector;

        public MyPsiElementVisitor(@NotNull ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(@NotNull PsiElement element) {
            Language language = element.getLanguage();

            if (language == XMLLanguage.INSTANCE) {
                visitXmlElement(element, holder, createLazyServiceCollector());
            } else if (language == YAMLLanguage.INSTANCE) {
                visitYamlElement(element, holder, createLazyServiceCollector());
            } else if (language == PhpLanguage.INSTANCE) {
                if (element instanceof StringLiteralExpression stringLiteralExpression) {
                    visitPhpElement(stringLiteralExpression, holder, createLazyServiceCollector());
                }
            }
        }

        private NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector> createLazyServiceCollector() {
            if (this.serviceCollector == null) {
                this.serviceCollector = NotNullLazyValue.lazy(() -> new ContainerCollectionResolver.LazyServiceCollector(holder.getProject()));
            }

            return this.serviceCollector;
        }
    }
}
