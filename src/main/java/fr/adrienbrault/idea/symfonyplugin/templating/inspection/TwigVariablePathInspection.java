package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigVariablePathInspection extends LocalInspectionTool {

    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiRecursiveElementVisitor(holder);
    }

    private static class MyPsiRecursiveElementVisitor extends PsiElementVisitor {

        @NotNull
        private final ProblemsHolder holder;

        MyPsiRecursiveElementVisitor(@NotNull ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            if(TwigPattern.getTypeCompletionPattern().accepts(element)) {
                visit(element);
            }
            super.visitElement(element);
        }

        private void visit(@NotNull PsiElement element) {
            Collection<String> beforeLeaf = TwigTypeResolveUtil.formatPsiTypeName(element);
            if(beforeLeaf.size() == 0) {
                return;
            }

            Collection<TwigTypeContainer> types = TwigTypeResolveUtil.resolveTwigMethodName(element, beforeLeaf);
            if(types.size() == 0) {
                return;
            }

            for(TwigTypeContainer twigTypeContainer: types) {
                PhpNamedElement phpNamedElement = twigTypeContainer.getPhpNamedElement();
                if(phpNamedElement == null) {
                    continue;
                }

                if(isWeakPhpClass(phpNamedElement)) {
                    return;
                }

                String text = element.getText();
                if(TwigTypeResolveUtil.getTwigPhpNameTargets(phpNamedElement, text).size() > 0) {
                    return;
                }
            }

            this.holder.registerProblem(element, "Field or method not found", ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
        }

        private boolean isWeakPhpClass(PhpNamedElement phpNamedElement) {
            return phpNamedElement instanceof PhpClass && (
                PhpElementsUtil.isInstanceOf((PhpClass) phpNamedElement, "ArrayAccess") ||
                PhpElementsUtil.isInstanceOf((PhpClass) phpNamedElement, "Iterator")
            );
        }
    }
}
