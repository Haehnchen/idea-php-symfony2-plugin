package fr.adrienbrault.idea.symfony2plugin.templating.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil

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
open class TwigVariablePathInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return MyPsiElementVisitor(holder)
    }

    private class MyPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
        private var typeCompletionPattern: ElementPattern<PsiElement>? = null

        override fun visitElement(element: PsiElement) {
            if (getTypeCompletionPattern().accepts(element)) {
                visit(element)
            }
            super.visitElement(element)
        }

        private fun visit(element: PsiElement) {
            if (TwigTypeResolveUtil.hasNextPsiTypeNameElement(element)) {
                return
            }

            val pathElements = TwigTypeResolveUtil.collectPsiTypeNameElementsWithCurrent(element)
            if (pathElements.size < 2) {
                return
            }

            val pathNames = pathElements.map(PsiElement::getText)

            val lastIndex = pathElements.size - 1
            val lastPathElement = pathElements[lastIndex]

            // Fast path: if the tail resolves, all earlier segments are already usable.
            val lastState = inspectPathElement(lastPathElement, pathNames.subList(0, lastIndex), lastPathElement.text)
            if (lastState == PathElementState.FOUND) {
                return
            }

            if (lastState == PathElementState.MISSING) {
                holder.registerProblem(lastPathElement, "Field or method not found", ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                return
            }

            // Tail is ambiguous; report the first earlier segment that is known missing.
            for (i in 1 until lastIndex) {
                val pathElement = pathElements[i]
                val state = inspectPathElement(pathElement, pathNames.subList(0, i), pathElement.text)
                if (state == PathElementState.MISSING) {
                    holder.registerProblem(pathElement, "Field or method not found", ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                    return
                }

                if (state == PathElementState.UNKNOWN) {
                    return
                }
            }
        }

        /**
         * Resolves the parent path and checks one Twig-visible name.
         */
        private fun inspectPathElement(element: PsiElement, beforeLeaf: Collection<String>, name: String): PathElementState {
            val types = TwigTypeResolveUtil.resolveTwigMethodName(element, beforeLeaf)
            if (types.isEmpty()) {
                return PathElementState.UNKNOWN
            }

            for (twigTypeContainer in types) {
                val phpClasses = TwigTypeResolveUtil.resolveTwigTypeClasses(element.project, twigTypeContainer)
                if (phpClasses.isEmpty()) {
                    return PathElementState.UNKNOWN
                }

                for (phpClass in phpClasses) {
                    if (TwigTypeResolveUtil.isWeakCollectionLikeClass(phpClass)) {
                        return PathElementState.UNKNOWN
                    }

                    if (TwigTypeResolveUtil.getTwigPhpNameTargets(phpClass, name).isNotEmpty()) {
                        return PathElementState.FOUND
                    }
                }
            }

            return PathElementState.MISSING
        }

        private fun getTypeCompletionPattern(): ElementPattern<PsiElement> {
            return typeCompletionPattern ?: TwigPattern.getTypeCompletionPattern().also { typeCompletionPattern = it }
        }

        /**
         * UNKNOWN means no reliable type information, so inspection stays silent.
         */
        private enum class PathElementState {
            FOUND,
            MISSING,
            UNKNOWN
        }
    }
}
