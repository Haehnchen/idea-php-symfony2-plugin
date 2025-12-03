package fr.adrienbrault.idea.symfony2plugin.codeInspection.command;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpReturn;
import com.jetbrains.php.lang.psi.elements.PhpTypedElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

/**
 * Inspection to detect invokable Symfony Commands that return non-integer values.
 *
 * Symfony 7.3+ invokable commands using #[AsCommand] attribute with __invoke() method
 * must return an integer exit code (e.g., Command::SUCCESS, Command::FAILURE).
 *
 * This inspection checks the actual return statements inside the method body.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class CommandInvokableReturnValueInspection extends LocalInspectionTool {
    public static final String MESSAGE = "Symfony: Command must return an integer value";

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof PhpReturn phpReturn) {
                    visitPhpReturn(phpReturn, holder);
                }
                super.visitElement(element);
            }
        };
    }

    private void visitPhpReturn(@NotNull PhpReturn phpReturn, @NotNull ProblemsHolder holder) {
        Method method = PsiTreeUtil.getParentOfType(phpReturn, Method.class);
        if (method == null || !"__invoke".equals(method.getName())) {
            return;
        }

        if (hasIntReturnType(method)) {
            return;
        }

        PhpClass phpClass = method.getContainingClass();
        if (phpClass == null || !hasAsCommandAttribute(phpClass)) {
            return;
        }

        PsiElement argument = phpReturn.getArgument();
        if (argument == null) {
            holder.registerProblem(
                phpReturn,
                MESSAGE,
                ProblemHighlightType.WARNING
            );
            return;
        }

        if (!isIntegerType(argument, holder)) {
            holder.registerProblem(
                argument,
                MESSAGE,
                ProblemHighlightType.WARNING
            );
        }
    }

    private boolean hasIntReturnType(@NotNull Method method) {
        PhpType returnType = method.getReturnType() != null ? method.getReturnType().getType() : null;
        if (returnType == null || returnType.isEmpty()) {
            return false;
        }

        for (String typeName : returnType.getTypes()) {
            String normalizedType = typeName.replace("\\", "").toLowerCase();
            if ("int".equals(normalizedType) || "integer".equals(normalizedType)) {
                return true;
            }
        }

        return false;
    }

    private boolean isIntegerType(@NotNull PsiElement element, @NotNull ProblemsHolder holder) {
        if (!(element instanceof PhpTypedElement typedElement)) {
            return false;
        }

        PhpType type = typedElement.getType();
        if (type.isEmpty()) {
            return false;
        }

        PhpIndex phpIndex = PhpIndex.getInstance(holder.getProject());
        PhpType completedType = phpIndex.completeType(holder.getProject(), type, new HashSet<>());

        for (String typeName : completedType.getTypes()) {
            String normalizedType = typeName.replace("\\", "").toLowerCase();
            if ("int".equals(normalizedType) || "integer".equals(normalizedType)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasAsCommandAttribute(@NotNull PhpClass phpClass) {
        return !phpClass.getAttributes("\\Symfony\\Component\\Console\\Attribute\\AsCommand").isEmpty();
    }
}
