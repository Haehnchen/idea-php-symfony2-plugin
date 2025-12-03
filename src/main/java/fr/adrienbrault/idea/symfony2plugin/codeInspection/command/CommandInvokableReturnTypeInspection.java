package fr.adrienbrault.idea.symfony2plugin.codeInspection.command;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpReturnType;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;

/**
 * Inspection to suggest adding an int return type to __invoke() method in Symfony Commands.
 *
 * Symfony 7.3+ invokable commands using #[AsCommand] attribute with __invoke() method
 * should declare int as return type for the exit code (e.g., Command::SUCCESS, Command::FAILURE).
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class CommandInvokableReturnTypeInspection extends LocalInspectionTool {
    public static final String MESSAGE = "Symfony: Consider adding int return type to command __invoke()";

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof PhpClass phpClass) {
                    visitPhpClass(phpClass, holder);
                }
                super.visitElement(element);
            }
        };
    }

    private void visitPhpClass(@NotNull PhpClass phpClass, @NotNull ProblemsHolder holder) {
        if (!hasAsCommandAttribute(phpClass)) {
            return;
        }

        Method invokeMethod = phpClass.findOwnMethodByName("__invoke");
        if (invokeMethod == null) {
            return;
        }

        if (!hasIntReturnType(invokeMethod)) {
            PsiElement problemElement = invokeMethod.getNameIdentifier();
            holder.registerProblem(
                problemElement != null ? problemElement : invokeMethod,
                MESSAGE,
                ProblemHighlightType.WEAK_WARNING
            );
        }
    }

    private boolean hasAsCommandAttribute(@NotNull PhpClass phpClass) {
        return !phpClass.getAttributes("\\Symfony\\Component\\Console\\Attribute\\AsCommand").isEmpty();
    }

    private boolean hasIntReturnType(@NotNull Method method) {
        PhpReturnType returnType = PhpPsiUtil.getChildByCondition(method, PhpReturnType.INSTANCEOF);
        if (returnType == null) {
            return false;
        }

        PhpType type = returnType.getType();
        if (type.isEmpty()) {
            return false;
        }

        for (String typeName : type.getTypes()) {
            String normalizedType = typeName.replace("\\", "").toLowerCase();
            if ("int".equals(normalizedType) || "integer".equals(normalizedType)) {
                return true;
            }
        }

        return false;
    }
}
