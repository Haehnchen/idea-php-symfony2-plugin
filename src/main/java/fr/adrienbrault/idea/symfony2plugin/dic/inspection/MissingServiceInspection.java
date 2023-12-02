package fr.adrienbrault.idea.symfony2plugin.dic.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceReference;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLLanguage;

import java.util.Arrays;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MissingServiceInspection extends LocalInspectionTool {

    public static final String INSPECTION_MESSAGE = "Symfony: Missing Service";

    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder holder;
        private ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector;

        MyPsiElementVisitor(@NotNull ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            if(element.getLanguage() == PhpLanguage.INSTANCE && element instanceof StringLiteralExpression) {
                // PHP
                MethodReference methodReference = PsiElementUtils.getMethodReferenceWithFirstStringParameter((StringLiteralExpression) element);
                if (methodReference != null && Arrays.stream(element.getReferences()).anyMatch(ref -> ref instanceof ServiceReference)) {
                    String serviceName = PhpElementsUtil.getFirstArgumentStringValue(methodReference);
                    if (StringUtils.isNotBlank(serviceName) && !hasService(serviceName)) {
                        holder.registerProblem(element, INSPECTION_MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                    }
                }

                // #[Autowire(service: 'foobar')]
                PsiElement leafText = PsiElementUtils.getTextLeafElementFromStringLiteralExpression((StringLiteralExpression) element);

                boolean isAttributeLeaf = leafText != null && (
                    PhpElementsUtil.getAttributeNamedArgumentStringPattern(ServiceContainerUtil.AUTOWIRE_ATTRIBUTE_CLASS, "service").accepts(leafText)
                        || PhpElementsUtil.getFirstAttributeStringPattern(ServiceContainerUtil.DECORATOR_ATTRIBUTE_CLASS).accepts(leafText)
                );

                if (isAttributeLeaf) {
                    String serviceName = ((StringLiteralExpression) element).getContents();
                    if (StringUtils.isNotBlank(serviceName) && !hasService(serviceName)) {
                        holder.registerProblem(element, INSPECTION_MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                    }
                }

            } else if(element.getLanguage() == YAMLLanguage.INSTANCE) {
                // yaml

                if (YamlElementPatternHelper.getServiceDefinition().accepts(element) && YamlElementPatternHelper.getInsideServiceKeyPattern().accepts(element)) {
                    String serviceName = YamlHelper.trimSpecialSyntaxServiceName(PsiElementUtils.getText(element));

                    // dont mark "@", "@?", "@@" escaping and expressions
                    if (serviceName.length() > 2 && !serviceName.startsWith("=") && !serviceName.startsWith("@") && !hasService(serviceName)) {
                        holder.registerProblem(element, INSPECTION_MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                    }
                }
            }

            super.visitElement(element);
        }

        private boolean hasService(@NotNull String serviceName) {
            if (this.lazyServiceCollector == null) {
                this.lazyServiceCollector = new ContainerCollectionResolver.LazyServiceCollector(holder.getProject());
            }

            return ContainerCollectionResolver.hasServiceName(lazyServiceCollector, serviceName);
        }
    }
}
