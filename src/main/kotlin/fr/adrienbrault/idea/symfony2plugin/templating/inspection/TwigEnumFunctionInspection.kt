package fr.adrienbrault.idea.symfony2plugin.templating.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.twig.TwigTokenTypes
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil

/**
 * Inspection for Twig enum() and enum_cases() functions to validate that:
 * - The provided class name exists
 * - The class is actually an enum type
 *
 * Examples:
 * {{ enum('App\\SomeEnum') }} - valid if SomeEnum is an enum
 * {{ enum('App\\NotAnEnum') }} - warning if NotAnEnum exists but is not an enum
 * {{ enum('App\\MissingClass') }} - error if class doesn't exist
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class TwigEnumFunctionInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return MyPsiElementVisitor(holder)
    }

    private class MyPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
        private val enumFunctionPattern by lazy(LazyThreadSafetyMode.NONE) {
            TwigPattern.getPrintBlockOrTagFunctionPattern("enum", "enum_cases")
        }

        override fun visitElement(element: PsiElement) {
            // Fast pre-filter: only STRING_TEXT elements can be enum/enum_cases arguments
            // enum('App\Config\SomeOption')
            // enum_cases('App\Config\SomeOption')
            if (element is LeafPsiElement &&
                element.node.elementType === TwigTokenTypes.STRING_TEXT &&
                enumFunctionPattern.accepts(element)
            ) {
                visitEnumFunction(element)
            }

            super.visitElement(element)
        }

        private fun visitEnumFunction(element: PsiElement) {
            val contents = element.text
            if (contents.isBlank()) {
                return
            }

            // Unescape backslashes: 'App\\Bike\\FooEnum' => 'App\Bike\FooEnum'
            val className = contents.replace("\\\\", "\\")

            val phpClass = PhpElementsUtil.getClassInterface(element.project, className)

            when {
                phpClass == null -> {
                    // Class doesn't exist
                    holder.registerProblem(
                        element,
                        "Missing class: $className",
                        ProblemHighlightType.WARNING
                    )
                }

                !phpClass.isEnum -> {
                    // Class exists but is not an enum
                    holder.registerProblem(
                        element,
                        "Class '${phpClass.name}' is not an enum",
                        ProblemHighlightType.WARNING
                    )
                }
            }
        }
    }
}
