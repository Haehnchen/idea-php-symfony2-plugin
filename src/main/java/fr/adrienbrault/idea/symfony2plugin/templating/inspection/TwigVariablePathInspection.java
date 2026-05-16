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
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reports unresolved Twig property or method path segments.
 *
 * Examples:
 * <ul>
 *   <li>{@code bar.unknown.public} highlights {@code unknown}</li>
 *   <li>{@code bar.getNext().apple.public} highlights {@code apple}</li>
 * </ul>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigVariablePathInspection extends LocalInspectionTool {

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
            if (TwigTypeResolveUtil.hasNextPsiTypeNameElement(element)) {
                return;
            }

            List<PsiElement> pathElements = TwigTypeResolveUtil.collectPsiTypeNameElementsWithCurrent(element);
            if (pathElements.size() < 2) {
                return;
            }

            List<String> pathNames = pathElements.stream().map(PsiElement::getText).collect(Collectors.toList());

            int lastIndex = pathElements.size() - 1;
            PsiElement lastPathElement = pathElements.get(lastIndex);

            // Fast path: if the tail resolves, all earlier segments are already usable.
            PathElementState lastState = inspectPathElement(lastPathElement, pathNames.subList(0, lastIndex), lastPathElement.getText());
            if (lastState == PathElementState.FOUND) {
                return;
            }

            if (lastState == PathElementState.MISSING) {
                this.holder.registerProblem(lastPathElement, "Field or method not found", ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                return;
            }

            // Tail is ambiguous; report the first earlier segment that is known missing.
            for (int i = 1; i < lastIndex; i++) {
                PsiElement pathElement = pathElements.get(i);
                PathElementState state = inspectPathElement(pathElement, pathNames.subList(0, i), pathElement.getText());
                if (state == PathElementState.MISSING) {
                    this.holder.registerProblem(pathElement, "Field or method not found", ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                    return;
                }

                if (state == PathElementState.UNKNOWN) {
                    return;
                }
            }
        }

        /**
         * Resolves the parent path and checks one Twig-visible name.
         */
        @NotNull
        private PathElementState inspectPathElement(@NotNull PsiElement element, @NotNull Collection<String> beforeLeaf, @NotNull String name) {
            Collection<TwigTypeContainer> types = TwigTypeResolveUtil.resolveTwigMethodName(element, beforeLeaf);
            if (types.isEmpty()) {
                return PathElementState.UNKNOWN;
            }

            for (TwigTypeContainer twigTypeContainer: types) {
                Collection<PhpClass> phpClasses = TwigTypeResolveUtil.resolveTwigTypeClasses(element.getProject(), twigTypeContainer);
                if (phpClasses.isEmpty()) {
                    return PathElementState.UNKNOWN;
                }

                for (PhpClass phpClass : phpClasses) {
                    if (TwigTypeResolveUtil.isWeakCollectionLikeClass(phpClass)) {
                        return PathElementState.UNKNOWN;
                    }

                    if (!TwigTypeResolveUtil.getTwigPhpNameTargets(phpClass, name).isEmpty()) {
                        return PathElementState.FOUND;
                    }
                }
            }

            return PathElementState.MISSING;
        }

        private ElementPattern<PsiElement> getTypeCompletionPattern() {
            return typeCompletionPattern != null ? typeCompletionPattern : (typeCompletionPattern = TwigPattern.getTypeCompletionPattern());
        }

        /**
         * UNKNOWN means no reliable type information, so inspection stays silent.
         */
        private enum PathElementState {
            FOUND,
            MISSING,
            UNKNOWN
        }
    }
}
