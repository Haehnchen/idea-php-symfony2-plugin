package fr.adrienbrault.idea.symfonyplugin.codeInspection.form;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtil;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfonyplugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfonyplugin.util.SymfonyUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormTypeAsClassConstantInspection extends LocalInspectionTool {

    public static String MESSAGE = "Use fully-qualified class name (FQCN)";

    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject()) || !SymfonyUtil.isVersionGreaterThenEquals(holder.getProject(), "2.8")) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder holder;

        MyPsiElementVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            if(!(element instanceof MethodReference) ||
                !ArrayUtil.contains(((MethodReference) element).getName(), "add", "create") ||
                !PhpElementsUtil.isMethodReferenceInstanceOf((MethodReference) element, "Symfony\\Component\\Form\\FormBuilderInterface")
                ) {

                super.visitElement(element);
                return;
            }

            PsiElement[] parameters = ((MethodReference) element).getParameters();
            if(parameters.length < 2) {
                super.visitElement(element);
                return;
            }

            if(!(parameters[1] instanceof StringLiteralExpression) || ((StringLiteralExpression) parameters[1]).getContents().contains("\\")) {
                super.visitElement(element);
                return;
            }

            holder.registerProblem(
                parameters[1],
                MESSAGE,
                ProblemHighlightType.WEAK_WARNING, new MyLocalQuickFix(parameters[1])
            );

            super.visitElement(element);
        }

        private static class MyLocalQuickFix extends LocalQuickFixOnPsiElement {

            MyLocalQuickFix(@NotNull PsiElement element) {
                super(element);
            }

            @NotNull
            @Override
            public String getText() {
                return "Use class constant";
            }

            @Nls
            @NotNull
            @Override
            public String getFamilyName() {
                return "Class constant";
            }

            @Override
            public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement psiElement, @NotNull PsiElement psiElement1) {
                if(!(getStartElement() instanceof StringLiteralExpression)) {
                    return;
                }

                try {
                    FormUtil.replaceFormStringAliasWithClassConstant((StringLiteralExpression) getStartElement());
                } catch (Exception ignored) {
                }
            }
        }
    }
}