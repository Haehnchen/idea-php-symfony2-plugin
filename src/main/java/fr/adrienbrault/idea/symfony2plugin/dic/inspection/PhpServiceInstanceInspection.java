package fr.adrienbrault.idea.symfony2plugin.dic.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.lang.psi.elements.ClassConstantReference;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.container.dict.ServiceTypeHint;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.inspection.intention.PhpServiceSuggestIntentionAction;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PHP array-style and fluent service config inspection for wrong service instance references.
 *
 * <pre>
 * MyService::class => [
 *     'arguments' => [service('<caret>'), '@<caret>'],
 * ]
 *
 * $services->set(MyService::class)->args([service('<caret>')]);
 * </pre>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpServiceInstanceInspection extends LocalInspectionTool {
    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        @NotNull
        private final ProblemsHolder holder;

        @Nullable
        private ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector;

        private MyPsiElementVisitor(@NotNull ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(@NotNull PsiElement psiElement) {
            if (psiElement instanceof StringLiteralExpression stringLiteral) {
                String contents = stringLiteral.getContents();
                if (StringUtils.isNotBlank(contents)) {
                    visitArgument(stringLiteral);
                }
            } else if (psiElement instanceof ClassConstantReference classConstantRef) {
                visitArgument(classConstantRef);
            }

            super.visitElement(psiElement);
        }

        private void visitArgument(@NotNull PsiElement argument) {
            ContainerCollectionResolver.LazyServiceCollector collector = getLazyServiceCollector(holder.getProject());

            ServiceTypeHint typeHint = ServiceContainerUtil.getPhpArrayConstructorTypeHint(argument, collector);
            if (typeHint == null) {
                typeHint = ServiceContainerUtil.getPhpFluentConstructorTypeHint(argument, collector);
            }

            if (typeHint == null) {
                return;
            }

            registerInstanceProblem(argument, holder, typeHint, collector);
        }

        private ContainerCollectionResolver.LazyServiceCollector getLazyServiceCollector(@NotNull Project project) {
            return this.lazyServiceCollector == null
                ? this.lazyServiceCollector = new ContainerCollectionResolver.LazyServiceCollector(project)
                : this.lazyServiceCollector;
        }
    }

    private static void registerInstanceProblem(
        @NotNull PsiElement argument,
        @NotNull ProblemsHolder holder,
        @NotNull ServiceTypeHint typeHint,
        @NotNull ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector
    ) {
        String serviceId;
        if (argument instanceof StringLiteralExpression stringLiteral) {
            String contents = stringLiteral.getContents();
            if (StringUtils.isBlank(contents)) {
                return;
            }
            // Strip '@' prefix for raw '@service_id' strings
            serviceId = contents.startsWith("@") ? contents.substring(1) : contents;
        } else if (argument instanceof ClassConstantReference classConstantRef) {
            serviceId = PhpElementsUtil.getClassConstantPhpFqn(classConstantRef);
        } else {
            return;
        }

        if (StringUtils.isBlank(serviceId)) {
            return;
        }

        PhpClass serviceClass = ServiceUtil.getResolvedClassDefinition(holder.getProject(), serviceId, lazyServiceCollector);
        if (serviceClass == null) {
            return;
        }

        Parameter[] parameters = typeHint.getMethod().getParameters();
        int index = typeHint.getIndex();
        if (index >= parameters.length) {
            return;
        }

        PhpClass expectedClass = PhpElementsUtil.getClassInterface(holder.getProject(), parameters[index].getDeclaredType().toString());
        if (expectedClass == null) {
            return;
        }

        if (!PhpElementsUtil.isInstanceOf(serviceClass, expectedClass)) {
            holder.registerProblem(
                argument,
                "Expect instance of: " + expectedClass.getPresentableFQN(),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                new PhpServiceSuggestIntentionAction(expectedClass.getFQN(), argument)
            );
        }
    }
}
