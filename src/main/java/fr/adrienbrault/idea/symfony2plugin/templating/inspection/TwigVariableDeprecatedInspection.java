package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.patterns.ElementPattern;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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

        private ElementPattern<PsiElement> typeCompletionPattern;

        MyPsiElementVisitor(@NotNull ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(@NonNull PsiElement element) {
            if (getTypeCompletionPattern().accepts(element)) {
                visit(element);
            }

            super.visitElement(element);
        }

        private void visit(@NotNull PsiElement element) {
            Collection<String> beforeLeaf = TwigTypeResolveUtil.formatPsiTypeName(element);
            if (beforeLeaf.isEmpty()) {
                return;
            }

            Collection<TwigTypeContainer> types = TwigTypeResolveUtil.resolveTwigMethodName(element, beforeLeaf);
            if (types.isEmpty()) {
                return;
            }

            String text = element.getText();
            Set<String> visitedTargets = new HashSet<>();

            for (TwigTypeContainer twigTypeContainer: types) {
                for (PhpClass phpClass : TwigTypeResolveUtil.resolveTwigTypeClasses(element.getProject(), twigTypeContainer)) {
                    for (PhpNamedElement namedElement : TwigTypeResolveUtil.getTwigPhpNameTargets(phpClass, text)) {
                        String targetKey = getDeprecatedTargetKind(namedElement) + ":" + phpClass.getFQN() + "::" + namedElement.getName();
                        if (visitedTargets.add(targetKey) && PhpElementsUtil.isClassOrFunctionDeprecated(namedElement)) {
                            this.holder.registerProblem(element, getDeprecatedMessage(phpClass, namedElement), ProblemHighlightType.LIKE_DEPRECATED);
                        }
                    }
                }
            }
        }

        @NotNull
        private String getDeprecatedTargetKind(@NotNull PhpNamedElement namedElement) {
            if (namedElement instanceof Method) {
                return "method";
            }

            if (namedElement instanceof Field) {
                return "field";
            }

            return "element";
        }

        @NotNull
        private String getDeprecatedMessage(@NotNull PhpClass phpClass, @NotNull PhpNamedElement namedElement) {
            if (namedElement instanceof Method) {
                return String.format("Method '%s::%s' is deprecated", phpClass.getName(), namedElement.getName());
            }

            if (namedElement instanceof Field) {
                return String.format("Field '%s::$%s' is deprecated", phpClass.getName(), namedElement.getName());
            }

            return String.format("Element '%s::%s' is deprecated", phpClass.getName(), namedElement.getName());
        }

        private ElementPattern<PsiElement> getTypeCompletionPattern() {
            return typeCompletionPattern != null ? typeCompletionPattern : (typeCompletionPattern = TwigPattern.getTypeCompletionPattern());
        }
    }
}
