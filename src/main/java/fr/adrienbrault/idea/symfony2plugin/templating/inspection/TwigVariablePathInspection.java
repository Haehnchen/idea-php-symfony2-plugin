package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.patterns.ElementPattern;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

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

        return new MyPsiElementVisitor(holder);
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        @NotNull
        private final ProblemsHolder holder;

        private ElementPattern<PsiElement> typeCompletionPattern;

        MyPsiElementVisitor(@NotNull ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(@NonNull PsiElement element) {
            if(getTypeCompletionPattern().accepts(element)) {
                visit(element);
            }
            super.visitElement(element);
        }

        private void visit(@NotNull PsiElement element) {
            Collection<String> beforeLeaf = TwigTypeResolveUtil.formatPsiTypeName(element);
            if(beforeLeaf.isEmpty()) {
                return;
            }

            Collection<TwigTypeContainer> types = TwigTypeResolveUtil.resolveTwigMethodName(element, beforeLeaf);
            if (types.isEmpty()) {
                return;
            }

            for (TwigTypeContainer twigTypeContainer: types) {
                String text = element.getText();
                Collection<PhpClass> phpClasses = TwigTypeResolveUtil.resolveTwigTypeClasses(element.getProject(), twigTypeContainer);
                if (phpClasses.isEmpty()) {
                    return;
                }

                for (PhpClass phpClass : phpClasses) {
                    if(TwigTypeResolveUtil.isWeakCollectionLikeClass(phpClass)) {
                        return;
                    }

                    if (!TwigTypeResolveUtil.getTwigPhpNameTargets(phpClass, text).isEmpty()) {
                        return;
                    }
                }
            }

            this.holder.registerProblem(element, "Field or method not found", ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
        }

        private ElementPattern<PsiElement> getTypeCompletionPattern() {
            return typeCompletionPattern != null ? typeCompletionPattern : (typeCompletionPattern = TwigPattern.getTypeCompletionPattern());
        }
    }
}
