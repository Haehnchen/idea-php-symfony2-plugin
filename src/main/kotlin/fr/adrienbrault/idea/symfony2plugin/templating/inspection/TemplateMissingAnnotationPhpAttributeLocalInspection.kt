package fr.adrienbrault.idea.symfony2plugin.templating.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpAttribute
import com.jetbrains.php.lang.psi.elements.PhpAttributesList
import de.espend.idea.php.annotation.util.AnnotationUtil
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpPsiAttributesUtil
import java.util.LinkedHashSet

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class TemplateMissingAnnotationPhpAttributeLocalInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is PhpDocTag) {
                    annotate(element, holder)
                }

                if (element is PhpAttribute) {
                    val fqn = element.fqn
                    if (fqn != null && PhpElementsUtil.isEqualClassName(fqn, *TwigUtil.TEMPLATE_ANNOTATION_CLASS)) {
                        annotate(element, holder)
                    }
                }

                super.visitElement(element)
            }
        }
    }

    private fun annotate(phpAttribute: PhpAttribute, holder: ProblemsHolder) {
        val templateNames = LinkedHashSet<String>()


        val isEmptyTemplateAndGuess = phpAttribute.arguments.isEmpty()
        if (isEmptyTemplateAndGuess) {
            val phpAttributesList = phpAttribute.parent
            if (phpAttributesList is PhpAttributesList) {
                val method = phpAttributesList.parent
                if (method is Method) {
                    templateNames.addAll(TwigUtil.getControllerMethodShortcut(method).asList())
                }
            }
        } else {
            val attributeDefaultValue = PhpPsiAttributesUtil.getAttributeValueByNameAsStringWithDefaultParameterFallback(phpAttribute, "template")
            if (attributeDefaultValue != null) {
                templateNames.add(attributeDefaultValue)
            }
        }

        if (templateNames.isNotEmpty()) {
            attachProblemForMissingTemplatesWithSuggestions(phpAttribute, holder, templateNames, isEmptyTemplateAndGuess)
        }
    }

    private fun annotate(phpDocTag: PhpDocTag, holder: ProblemsHolder) {
        val phpDocAnnotationContainer = AnnotationUtil.getPhpDocAnnotationContainer(phpDocTag)
        if (phpDocAnnotationContainer == null || !PhpElementsUtil.isEqualClassName(phpDocAnnotationContainer.phpClass, *TwigUtil.TEMPLATE_ANNOTATION_CLASS)) {
            return
        }

        val phpDocAttrList = phpDocTag.firstPsiChild
        if (phpDocAttrList == null) {
            return
        }

        val templateNames = LinkedHashSet<String>()

        var isEmptyTemplateAndGuess = false

        val matcher = AnnotationUtil.getPropertyValueOrDefault(phpDocTag, "template")
        if (matcher != null) {
            templateNames.add(matcher)
        } else {
            isEmptyTemplateAndGuess = true

            // find template name on last method
            val docComment = PsiTreeUtil.getParentOfType(phpDocTag, PhpDocComment::class.java) ?: return

            val method = PsiTreeUtil.getNextSiblingOfType(docComment, Method::class.java) ?: return

            templateNames.addAll(TwigUtil.getControllerMethodShortcut(method).asList())
        }

        if (templateNames.isNotEmpty()) {
            attachProblemForMissingTemplatesWithSuggestions(phpDocTag, holder, templateNames, isEmptyTemplateAndGuess)
        }
    }

    private fun attachProblemForMissingTemplatesWithSuggestions(
        target: PsiElement,
        holder: ProblemsHolder,
        templateNames: LinkedHashSet<String>,
        isEmptyTemplateAndGuess: Boolean
    ) {
        if (templateNames.isEmpty()) {
            return
        }

        for (templateName in templateNames) {
            if (TwigUtil.getTemplateFiles(holder.project, templateName).isNotEmpty()) {
                return
            }
        }

        // find html target, as this this our first priority for end users condition
        // or fallback on first item
        val templates = templateNames.filter { it.lowercase().endsWith(".html.twig") }.toTypedArray()

        val quickFixes = ArrayList<LocalQuickFix>()
        quickFixes.add(TemplateCreateByNameLocalQuickFix(*templates))

        if (!isEmptyTemplateAndGuess && templates.isNotEmpty()) {
            // use first as underscore is higher priority and common way by framework bundle
            quickFixes.add(TemplateGuessTypoQuickFix(templates[0]))
        }

        holder.registerProblem(target, "Twig: Missing Template", *quickFixes.toTypedArray())
    }
}
