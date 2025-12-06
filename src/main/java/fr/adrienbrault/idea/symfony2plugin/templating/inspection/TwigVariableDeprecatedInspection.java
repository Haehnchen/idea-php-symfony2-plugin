package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.lang.psi.elements.Method;
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
public class TwigVariableDeprecatedInspection extends LocalInspectionTool {

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

        MyPsiElementVisitor(@NotNull ProblemsHolder holder) {
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
            if(beforeLeaf.isEmpty()) {
                return;
            }

            Collection<TwigTypeContainer> types = TwigTypeResolveUtil.resolveTwigMethodName(element, beforeLeaf);
            if(types.isEmpty()) {
                return;
            }

            for(TwigTypeContainer twigTypeContainer: types) {
                PhpNamedElement phpClass = twigTypeContainer.getPhpNamedElement();
                if(!(phpClass instanceof PhpClass)) {
                    continue;
                }

                String text = element.getText();

                for (PhpNamedElement namedElement : TwigTypeResolveUtil.getTwigPhpNameTargets(phpClass, text)) {
                    if(namedElement instanceof Method method && PhpElementsUtil.isClassOrFunctionDeprecated(method)) {
                        this.holder.registerProblem(element, String.format("Method '%s::%s' is deprecated", phpClass.getName(), namedElement.getName()), ProblemHighlightType.LIKE_DEPRECATED);
                    }
                }
            }
        }
    }
}
